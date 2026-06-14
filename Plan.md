# Plan: Implement Spring Security 7 + OAuth2 + Multi-Factor Authentication (MFA)

This document is a pragmatic, step-by-step implementation and test plan tailored to this repository (Java 21, Spring Boot 4.0.6, Authorization Server + JPA). Keep changes small and reversible. Follow the checklist, then the step details and test cases.

Checklist (do these in order)
- [ ] Verify Spring Boot / Spring Security compatibility and choose the correct Spring Security 7 (or latest compatible) artifact versions
- [ ] Add new DB fields (MFA metadata) and prepare a simple migration (or JPA field additions)
- [ ] Add MFA domain/service code: TOTP generation/verification, backup codes, SMS adapter interface
- [ ] Wire MFA into authentication flow (authorization_code & form-login flows) via additional filter/handler
- [ ] Ensure JWT access tokens include MFA claim (mfa_verified or mfa_level)
- [ ] Add API endpoints for enabling/disabling MFA and for verification flows
- [ ] Update `ConfigClass` to support authorization_code grant + required filter chain changes
- [ ] Add tests (unit + integration) and run them
- [ ] Document runtime configuration (Twilio keys, rate limits, key persistence)

Notes on compatibility and risk
- This repo currently uses Spring Boot `4.0.6` and the Spring Authorization Server dependencies. Confirm which Spring Security major release is packaged by the chosen Spring Boot version. If a breaking upgrade is required, plan a separate dependency-compatibility pass and run the full test suite before MFA work.
- MFA adds two-step complexity. Start with TOTP (time-based OTP) because it is self-contained and does not require external vendors.

High-level design
- Primary authentication: existing username + password flow handled by `UserService` and `ConfigClass`.
- Secondary authentication (MFA): after primary authentication succeeds, enforce a second factor when the user has MFA enabled. Supported methods:
  - TOTP (Google Authenticator / Authenticator apps)
  - SMS (pluggable adapter; provider-specific implementation stored behind an interface)
  - Backup codes (one-time use) for account recovery
- Authorization server integration: for `authorization_code` and browser flows, the authorization server must pause the flow after successful password authentication and present an MFA challenge. For token endpoint flows (client_credentials), MFA is not applicable.
- Token claims: JWTs produced by the Authorization Server should include a claim reflecting MFA status (for example `mfa_verified: true/false` or `mfa_level: 0|1`). Resource servers may rely on this claim.

Required code / schema changes (concrete)
1) Users entity (`src/main/java/.../Userfolder/Users.java`)
   - Add fields (Lombok + validation):
     - `private boolean mfaEnabled;`
     - `private String mfaSecret;`  // base32 secret for TOTP, nullable
     - `private String phone;`      // for SMS (add @Pattern if desired)
     - `private ArrayList<String> backupCodes;` // persisted as JSON or a separate table
     - `private String mfaMethod;` // e.g., "TOTP", "SMS" (or enum)
   - Keep Lombok annotations as required by project conventions.

2) Database migration
   - If you use a migration system (recommended), add a migration to add these columns. Minimal SQL (Oracle) examples:
     - `ALTER TABLE Users_for_authentication ADD (mfa_enabled NUMBER(1) DEFAULT 0, mfa_secret VARCHAR2(255), phone VARCHAR2(32), mfa_method VARCHAR2(16));`
     - For `backupCodes`, consider a new table `user_backup_codes(user_id, code_hash, used NUMBER(1))`.

3) New MFA service and helpers
   - `src/main/java/.../security/MfaService.java` (interface + implementation)
     - Methods: `String generateTotpSecret();`, `String getProvisioningUri(username, secret);`, `boolean verifyTotpCode(secret, code);`, `List<String> generateBackupCodes(count)`, `boolean consumeBackupCode(userId, code)`
   - Use a well-tested TOTP implementation (e.g., Google's TOTP algorithm via a small helper or an external library). If no external library allowed, implement RFC 6238 verification.
   - `SmsSender` interface + `TwilioSmsSender` implementation (pluggable). Keep provider credentials in `application.properties` or secrets store.

4) Authentication flow changes
   - Add a new Filter / AuthenticationSuccessHandler / AuthenticationConverter chain that detects post-authentication when the user has MFA enabled and requires second factor before completing authentication for interactive flows.
   - File changes:
     - `Configuration/ConfigClass.java`:
       - Add the `authorization_code` grant to the `registeredClientRepository()` configuration (currently only CLIENT_CREDENTIALS & REFRESH_TOKEN are configured). Update TokenSettings/ClientSettings accordingly.
       - Add a custom `SecurityFilterChain` or modify existing chains to include an `MfaFilter` (ordered after the username/password auth is successful but before issuing authorization).
     - New `security/MfaFilter.java` (or `MfaAuthenticationFilter`) that:
       - Intercepts successful primary authentication
       - If `user.isMfaEnabled()`, redirect to an MFA verification endpoint or return a 401 with an MFA challenge (JSON) for SPA/API clients.
       - On verification success, create a new Authentication token that contains `mfa_verified=true` and continues the authorization flow.

5) Token customization
   - Add a JWT claim (for example `mfa_verified`) when issuing tokens. Implement a custom `OAuth2TokenCustomizer<JwtEncodingContext>` bean that checks the Authentication and injects `mfa_verified:true` when appropriate.

6) API Endpoints
   - `POST /mfa/setup`  — starts setup (generates secret + provisioning URI for TOTP). Protected endpoint (authenticated).
   - `POST /mfa/verify` — verify the code during setup, enables MFA on success.
   - `POST /mfa/challenge/sms` — request SMS code (if SMS enabled)
   - `POST /mfa/confirm` — confirm MFA code during login flow.
   - Keep these in a new controller: `src/main/java/.../Userfolder/MfaController.java`.

7) UI / Redirects
   - For browser flows: simple HTML or Thymeleaf templates under `src/main/resources/templates/mfa/` for entering TOTP or SMS code. The Authorization Server default login redirect should be wired to these pages when MFA is required.
   - For API clients: respond with a JSON MFA challenge and allow clients to call `/mfa/confirm` to continue.

8) Persistence of keys / JWKs
   - Current `ConfigClass` generates RSA keys on every startup. For token continuity, externalize keys to a file or a secrets store and load at startup. This is orthogonal but recommended before adding MFA claims to tokens in production.

9) RegisteredClient changes
   - Modify `registeredClientRepository()` in `ConfigClass` to include `AuthorizationGrantType.AUTHORIZATION_CODE` and `ClientSettings` to allow redirect URIs and PKCE if needed.

Testing strategy and concrete tests
Unit tests (fast)
- `MfaServiceTest`
  - testGenerateTotpSecret_shouldReturnNonNullSecret()
  - testVerifyTotpCode_validCode_returnsTrue() — use known TOTP seed + codes or stub time provider
  - testBackupCodes_generateAndConsume()` — generated codes are hashed, consume marks them used

- `UserServiceTest` (existing pattern)
  - testSave_encodesPassword(): verify `passwordEncoder().encode()` called and repository.save receives encoded password
  - testLoadUserByUsername_authoritiesMapping(): verify roles -> `ROLE_{SCOPE}` mapping

Integration tests (slower)
- `MfaLoginIntegrationTest` (use `@SpringBootTest` + test config or Testcontainers DB/H2 alternative)
  - scenario: user with MFA disabled can authenticate and obtain authorization code / token
  - scenario: user with MFA enabled, after username/password, receives MFA challenge; submitting valid TOTP completes flow and returns tokens containing `mfa_verified:true` claim
  - scenario: invalid TOTP prevents token issuance
  - scenario: backup code can be used exactly once and then is invalid

- `AuthorizationServerRegisteredClientTest`
  - testAuthorizationCodeGrant_flow() — register a client with auth_code grant, perform authorize & token exchange, assert access token

Testing notes / environment
- Tests currently use `application.properties` (Oracle). For CI use, add `@TestPropertySource` to point to an in-memory H2 (or Testcontainers Oracle) to avoid requiring a running Oracle instance.
- For tests that use SMS, use a mock `SmsSender` implementation and assert invocations (do not call real provider in tests).

Implementation milestones & rough estimates
- Milestone A — Dependency & compatibility check (0.25 day)
- Milestone B — DB/entity changes + migration (0.5 day)
- Milestone C — `MfaService` + unit tests (0.5–1 day)
- Milestone D — Authentication flow wiring + token customization (1–1.5 days)
- Milestone E — Endpoints + UI minimal pages + integration tests (1 day)
- Milestone F — Hardening, key persistence, documentation (0.5 day)

Acceptance criteria
- Automated unit tests pass locally
- Integration test demonstrates end-to-end authorization_code flow with MFA (TOTP)
- JWTs contain `mfa_verified` claim when MFA satisfied
- Admin can enable/disable MFA for a user via API

Security, privacy and operational notes
- Store `mfaSecret` and backup codes carefully. If backup codes are stored, store only hashed values.
- Rate limit MFA verification attempts and SMS sends. Add logging/monitoring for suspicious attempts.
- Consider rotating RSA JWK keys with care — token invalidation will occur on rotation.

Developer quick-commands
```powershell
# Run unit tests
./gradlew test

# Run app locally
./gradlew bootRun

# Build fat jar
./gradlew build
java -jar build/libs/AuthServer-2.1.jar
```

Files you will touch (quick reference)
- `src/main/java/com/karthikProjects/AuthServer/Configuration/ConfigClass.java` — register auth_code grant, insert MfaFilter, token customizer
- `src/main/java/com/karthikProjects/AuthServer/Userfolder/Users.java` — add MFA fields
- `src/main/java/com/karthikProjects/AuthServer/Userfolder/MfaController.java` — endpoints for setup/verify/challenge
- `src/main/java/com/karthikProjects/AuthServer/security/MfaService.java` — TOTP and backup code logic
- `src/test/java/...` — MfaServiceTest, MfaLoginIntegrationTest, AuthorizationServerRegisteredClientTest

If you want, I can now:
- produce the exact patch that adds the new fields to `Users.java` and a starter `MfaService` + unit tests, or
- create the skeleton controller and filter and wire into `ConfigClass` with TODOs for TOTP implementation.

---
End of Plan.md

