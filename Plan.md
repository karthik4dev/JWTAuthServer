# Plan: Implement Spring Security 7 + OAuth2 + Multi-Factor Authentication (MFA)

This document is a pragmatic, step-by-step implementation and test plan tailored to this repository (Java 21, Spring Boot 4.0.6, Authorization Server + JPA). Keep changes small and reversible. Follow the checklist, then the step details and test cases.

**Project Version**: 2.1 | **Last Updated**: June 19, 2026

---

## Current Project Status (Detailed Review)

### ✓ COMPLETED COMPONENTS

#### Core Infrastructure
- **Spring Boot 4.0.6** with Java 21 toolchain properly configured (build.gradle lines 10-14)
- **OAuth2 Authorization Server** infrastructure fully set up (ConfigClass.java)
- **Two SecurityFilterChains** configured with correct ordering:
  - Order(1): Authorization server endpoints (OAuth2 token issuance, OIDC)
  - Order(2): Default form login (browser-based user authentication)
- Both chains permit `/saveuser` endpoint without authentication (public user registration)
- CSRF disabled on both chains (appropriate for stateless API-first design)

#### User Management & Authentication
- **Users Entity** (Users.java): Fully implemented with Lombok annotations
  - @Entity, @Builder, @AllArgsConstructor, @NoArgsConstructor, @Getter, @Setter
  - Current fields: id (SEQUENCE), username (unique, @NonNull), password (@NonNull, BCrypt hash), mail (@NonNull, email regex validation), roles (ArrayList<Scopes>, @NonNull)
  - JPA table: `Users_for_authentication`
- **UserService** (UserService.java): Implements UserDetailsService correctly
  - loadUserByUsername(): Loads users from Oracle DB, maps Scopes to Spring Security authorities (ROLE_READ, ROLE_ADMIN)
  - save(): Encodes passwords via BCryptPasswordEncoder, persists to repository
- **UserController** (UserController.java): REST endpoint for user registration
  - POST /saveuser (public, JSON payload)
- **UserRepository** (UserRepository.java): Custom JPA repository
  - findByUsername(String): Returns Optional<Users>

#### Security & Cryptography
- **Password Encoding**: BCryptPasswordEncoder implemented as static Bean in ConfigClass
- **RSA-2048 Key Generation**: Implemented via ConfigClass.generateRsaKey()
  - Generates new key pair on every application startup
  - Keys support JWT signing for OAuth2 tokens
- **JWT Decoder**: Configured via JwtDecoder Bean (uses KeySource)
- **RegisteredClient**: In-memory client repository with client_id="client1"
  - Current grant types: CLIENT_CREDENTIALS, REFRESH_TOKEN
  - Scopes: READ, ADMIN
  - Token TTL: 24 hours

#### Database & ORM
- **Oracle Database** connection with HikariCP pooling
  - URL: jdbc:oracle:thin:@localhost:1521/xepdb1
  - Credentials: application_User / application@123
  - Pool size: 5-20 connections
  - Driver: oracle.jdbc.OracleDriver
- **JPA/Hibernate Configuration**:
  - Platform: OracleDialect
  - DDL mode: `update` (auto-creates/updates schema)
  - SQL logging enabled (show_sql=true)

#### Scopes & Authorization
- **Scopes Enum** (Scopes.java): Two-value enum (READ, ADMIN)
  - Users.roles is ArrayList<Scopes>
  - Mapped to Spring Security authorities as ROLE_{SCOPE}

#### Testing Foundation
- **AuthServerApplicationTests**: Context loading test with @SpringBootTest
  - Verifies application can start without errors
- **MockitoTestClasses**: Mocked UserRepository tests
  - testAssertMockitoTest(): Verifies UsernameNotFoundException thrown for missing user
  - Uses @MockitoBean for repository mocking
  - Verifies method invocation counts

#### Build & Deployments
- **Gradle Build** (build.gradle):
  - Java 21 toolchain configured
  - All required dependencies present (OAuth2 Authorization Server, Spring Security, JOSE, JPA, Lombok, Mockito)
  - Spring Boot 4.0.6
- **Docker Support** (dockerfile):
  - Image build with JAR_VERSION parameter
  - Container exposes port 9000

#### Documentation & Configuration
- **Logging Levels**: 
  - Spring Security: DEBUG
  - Hibernate: INFO
- **Server Port**: 9000
- **Application Properties**: Comprehensive configuration for DB, JPA, and security

---

### ⚠ INCOMPLETE / NEEDS WORK

#### MFA Implementation (High Priority)
- [ ] **Database Schema Updates**
  - [ ] Add MFA-related columns to Users_for_authentication table
  - [ ] Create USER_BACKUP_CODES table for recovery codes
  - [ ] Create migration script or manual ALTER TABLE statements
  
- [ ] **Users Entity Extension**
  - [ ] Add mfaEnabled: boolean (default false)
  - [ ] Add mfaSecret: String (nullable, base32 encoded for TOTP)
  - [ ] Add mfaMethod: String or enum (TOTP, SMS, BACKUP)
  - [ ] Add phone: String (nullable, for SMS MFA)
  - [ ] Add backupCodes: Collection or separate table relationship
  - [ ] Add mfaSetupDate: LocalDateTime (nullable, tracks when MFA was enabled)
  - [ ] Keep all Lombok annotations as project convention

- [ ] **MFA Service Implementation**
  - [ ] Create MfaService interface with methods:
    - [ ] generateTotpSecret(): String (generates base32-encoded TOTP secret)
    - [ ] getProvisioningUri(String username, String secret): String (for QR codes)
    - [ ] verifyTotpCode(String secret, String code): boolean (RFC 6238 verification)
    - [ ] generateBackupCodes(int count): List<String> (typically 10 codes)
    - [ ] consumeBackupCode(Long userId, String code): boolean (one-time use)
    - [ ] validateMfaState(Users user): MfaValidationResult (checks if MFA is properly configured)
  - [ ] Implement using time-based OTP algorithm (can use external library or RFC 6238 implementation)
  - [ ] Handle time skew tolerance (±30 seconds) for TOTP validation

- [ ] **SMS Adapter (Pluggable)**
  - [ ] Create SmsSender interface with method: sendCode(String phoneNumber, String code): void
  - [ ] Implement TwilioSmsSender (if Twilio chosen)
  - [ ] Add rate limiting for SMS sends (e.g., max 5 codes per phone per hour)
  - [ ] Collect Twilio API credentials for application.properties

- [ ] **Authentication Flow Integration**
  - [ ] Create MfaFilter (or AuthenticationSuccessHandler extension)
    - [ ] Intercepts successful primary authentication
    - [ ] Checks if user has MFA enabled (user.isMfaEnabled())
    - [ ] If MFA enabled: Redirect to MFA challenge endpoint instead of completing auth
    - [ ] If MFA disabled: Continue normal authorization flow
  - [ ] Wire MfaFilter into ConfigClass security chains
  - [ ] Handle both browser (HTML redirect) and API (JSON challenge) flows

- [ ] **JWT Token Customization**
  - [ ] Implement OAuth2TokenCustomizer<JwtEncodingContext> Bean
  - [ ] Add mfa_verified claim (true/false) to JWT payload
  - [ ] Add mfa_level claim (0=no MFA, 1=TOTP verified, etc.)
  - [ ] Include MFA method in token if applicable

- [ ] **Authorization Code Grant Type**
  - [ ] Add AuthorizationGrantType.AUTHORIZATION_CODE to RegisteredClient (ConfigClass)
  - [ ] Add valid redirect URIs in ClientSettings
  - [ ] Add post-logout redirect URIs
  - [ ] Configure consent approval screen if needed

- [ ] **MFA API Endpoints** (New MfaController)
  - [ ] POST /mfa/setup (authenticated) — Initiates TOTP setup, returns secret + provisioning URI
  - [ ] POST /mfa/setup/verify (authenticated) — Verifies TOTP code during setup, enables MFA
  - [ ] POST /mfa/setup/sms (authenticated) — Initiates SMS setup, sends test code
  - [ ] POST /mfa/disable (authenticated) — Disables MFA for user (requires password verification)
  - [ ] POST /mfa/challenge/totp (unauthenticated) — Verifies TOTP during login
  - [ ] POST /mfa/challenge/sms (unauthenticated) — Requests SMS code during login
  - [ ] POST /mfa/challenge/backup (unauthenticated) — Uses backup code for recovery
  - [ ] GET /mfa/backup-codes (authenticated) — Returns current backup codes
  - [ ] POST /mfa/backup-codes/regenerate (authenticated) — Regenerates backup codes

- [ ] **UI Templates** (Thymeleaf or simple HTML)
  - [ ] src/main/resources/templates/mfa/setup.html — TOTP secret + QR code display
  - [ ] src/main/resources/templates/mfa/verify.html — Input field for TOTP/SMS code during setup
  - [ ] src/main/resources/templates/mfa/challenge.html — MFA code entry during login
  - [ ] src/main/resources/templates/mfa/backup.html — Backup code selection (if multiple methods)

- [ ] **Comprehensive Unit Tests** (Fast, mocked)
  - [ ] MfaServiceTest:
    - [ ] testGenerateTotpSecret_returnsNonNull()
    - [ ] testGenerateTotpSecret_returnsBase32Encoded()
    - [ ] testVerifyTotpCode_validCode_returnsTrue()
    - [ ] testVerifyTotpCode_invalidCode_returnsFalse()
    - [ ] testVerifyTotpCode_expiredCode_returnsFalse()
    - [ ] testVerifyTotpCode_toleratesTimeSkew()
    - [ ] testGenerateBackupCodes_returnsExpectedCount()
    - [ ] testBackupCodes_consumedCodeCannotBeReuseD()
  - [ ] UserServiceTest (extend existing):
    - [ ] testSave_encodesPassword()
    - [ ] testLoadUserByUsername_mapsScopesToAuthorities()
  - [ ] ConfigClassTest:
    - [ ] testPasswordEncoder_encodesDifferently()
    - [ ] testJwtDecoder_createdSuccessfully()

- [ ] **Integration Tests** (Slower, with @SpringBootTest)
  - [ ] MfaAuthenticationTest:
    - [ ] testUserWithoutMfa_cannotAcessAfterPasswordAuth()
    - [ ] testUserWithMfaE nabled_receivesChallenge()
    - [ ] testMfaChallenge_validTotp_completes()
    - [ ] testMfaChallenge_invalidTotp_rejectsCompletion()
    - [ ] testBackupCode_actsAsRecoveryMechanism()
  - [ ] AuthorizationCodeFlowTest:
    - [ ] testAuthorizationCodeGrant_withoutMfa_flow()
    - [ ] testAuthorizationCodeGrant_withMfa_flow()
    - [ ] testAuthorizationCodeGrant_mfaTokenContainsClaim()
  - [ ] SmsAdapterTest (mock Twilio):
    - [ ] testSmsSender_callsProviderCorrectly()
    - [ ] testSmsRateLimiting_enforced()

#### RSA Key Persistence (Medium Priority)
- [ ] Implement persistent RSA key storage
  - [ ] Option 1: Store keys in database (RSA_KEYS table)
  - [ ] Option 2: Store keys in encrypted file (PKCS#12 format)
  - [ ] Option 3: Use external secrets manager (AWS Secrets Manager, HashiCorp Vault)
  - [ ] Load keys at startup, only generate new if none found
  - [ ] Ensures JWT tokens remain valid across application restarts

#### Empty/Placeholder Files (Low Priority)
- [ ] **Roles.java**: Currently empty enum. Consider if this should be used instead of Scopes, or remove if redundant.
- [ ] **UserPrincipal.java**: Currently empty class. If needed, implement custom UserPrincipal extending Spring Security's User
- [ ] **RoleIsNotCorrectException.java**: Currently empty exception. Consider removing if not used.

#### Configuration & Environment (Medium Priority)
- [ ] Add Twilio credentials to application.properties (if SMS MFA chosen)
  - [ ] twilio.account-sid=xxx
  - [ ] twilio.auth-token=xxx
  - [ ] twilio.from-number=xxxx
- [ ] Add rate limiting configuration
  - [ ] mfa.rate-limit.totp-attempts=5
  - [ ] mfa.rate-limit.sms-sends=5
  - [ ] mfa.rate-limit.window-minutes=15
- [ ] Add MFA configuration flags
  - [ ] mfa.enabled=true
  - [ ] mfa.totp-enabled=true
  - [ ] mfa.sms-enabled=false
  - [ ] mfa.backup-codes-count=10
- [ ] Add JWT claim configuration
  - [ ] jwt.mfa-claim-name=mfa_verified
  - [ ] jwt.mfa-level-enabled=true

---

## Dependency & Compatibility Status

### Current Versions
| Component | Version | Status |
|-----------|---------|--------|
| Spring Boot | 4.0.6 | ✓ Current |
| Java | 21 (toolchain) | ✓ Current |
| Spring Security | 6.3.x (bundled with SB 4.0.6) | ✓ Current |
| OAuth2 Authorization Server | 1.2.x (bundled) | ✓ Current |
| Spring Security JOSE (JWT) | 6.3.x | ✓ Current |
| Spring Data JPA | 3.3.x (bundled) | ✓ Current |
| Hibernate | 6.4.x | ✓ Current |
| Lombok | Latest | ✓ Current |
| Mockito | Latest (test) | ✓ Current |
| JUnit 5 | 5.10.x (bundled) | ✓ Current |
| OracleJDBC (ojdbc11) | 21.x | ✓ Current |

### Risk Assessment
- **Low Risk**: No version upgrades required for MFA implementation
- **All dependencies are current and compatible** with Java 21 and Spring Boot 4.0.6
- **TOTP Implementation**: No external vendor required; can use RFC 6238 algorithm or lightweight library (e.g., google-authenticator, commons-codec)
- **SMS Implementation**: Pluggable via SmsSender interface; Twilio is optional enhancement

---

## Architecture & Design

### Current Authentication Flow (Implemented ✓)
```
1. User Registration Request
   └─→ POST /saveuser
       └─→ UserController.CreateUsers()
           └─→ UserService.save()
               └─→ Password hashing (BCryptPasswordEncoder)
               └─→ UserRepository.save()
               └─→ Persists to Users_for_authentication table

2. Token Request (OAuth2 Client Credentials Flow)
   └─→ POST /oauth2/token
       └─→ spring-security-oauth2-authorization-server internals
           └─→ Spring Security loads user credentials
           └─→ DaoAuthenticationProvider validates (via UserService)
           └─→ ConfigClass.authenticationProvider() checks password
           └─→ RegisteredClientRepository validates client
           └─→ JWT token issued with RSA-2048 signature
               └─→ Token TTL: 24 hours

3. Token Validation (on Resource Server)
   └─→ JwtDecoder validates signature
   └─→ Token claims extracted and used for authorization
```

### MFA Enhancement Design (To Be Implemented)

#### Authentication Flow with MFA
```
Primary Authentication (Username + Password)
  ↓
UserService.loadUserByUsername() + DaoAuthenticationProvider [✓ DONE]
  ↓
Check user.isMfaEnabled() [NEW - to be implemented]
  ↓
IF MFA disabled:
  └─→ Complete normal authorization flow → Issue JWT without MFA claim
     
IF MFA enabled:
  └─→ MfaFilter intercepts authentication
      └─→ Return 401 + MFA Challenge (JSON/HTML redirect based on client type)
  └─→ User submits MFA code to /mfa/challenge/{method} endpoint
      └─→ MfaService validates code
          └─→ If valid: Create new Authentication with mfa_verified=true
          └─→ If invalid: Reject, log attempt for rate limiting
  └─→ Complete authorization flow → Issue JWT with mfa_verified=true claim
```

#### Supported MFA Methods
| Method | Implementation | Priority | Status |
|--------|----------------|----------|--------|
| TOTP (Google Authenticator) | RFC 6238 time-based OTP | HIGH | Planned |
| SMS | Twilio API adapter | MEDIUM | Planned |
| Backup Codes | Hashed one-time codes | HIGH | Planned |
| Email OTP | Future enhancement | LOW | Not planned yet |
| Hardware Keys (FIDO2) | Future enhancement | LOW | Not planned yet |

#### Token Claims for MFA
```json
{
  "target_audience": "user",
  "sub": "user123",
  "username": "john.doe",
  "scope": ["ROLE_READ", "ROLE_ADMIN"],
  "mfa_verified": true,
  "mfa_level": 1,
  "mfa_method": "TOTP",
  "mfa_verified_at": 1718808000,
  "exp": 1718894400,
  "iat": 1718808000
}
```

### Grant Types Configuration

#### Current Status (ConfigClass.registeredClientRepository())
- ✓ **CLIENT_CREDENTIALS**: Implemented (service-to-service)
- ✓ **REFRESH_TOKEN**: Implemented (token refresh)
- ✗ **AUTHORIZATION_CODE**: Not yet implemented (browser-based with MFA)

#### Authorization Code Flow (To Be Added)
```
1. Client initiates login
   └─→ GET /oauth2/authorize?client_id=client1&response_type=code&redirect_uri=...

2. User enters credentials + MFA if enabled
   └─→ POST /login
   └─→ MFA challenge if mfaEnabled=true

3. Spring Security redirects to approve consent screen (if required)

4. Authorization code returned
   └─→ Redirect to redirect_uri?code=XXXXX

5. Client exchanges code for token
   └─→ POST /oauth2/token
       with client_id, client_secret, code, grant_type=authorization_code

6. JWT access token returned with MFA claims
```

---

## Detailed Implementation Plan

### Phase 1: Database & Entity Layer (Est. 0.5-1 day)

#### Step 1a: Database Schema Extension
**Files**: Migration script or manual ALTER TABLE
**Tasks**:
- Add columns to `Users_for_authentication`:
  - `mfa_enabled NUMBER(1) DEFAULT 0` — Flag for MFA activation
  - `mfa_method VARCHAR2(16)` — Method: TOTP, SMS, NONE
  - `mfa_secret VARCHAR2(255)` — Base32-encoded TOTP secret
  - `phone VARCHAR2(20)` — Phone for SMS delivery
  - `mfa_setup_date TIMESTAMP` — When MFA was enabled
- Create table `USER_BACKUP_CODES`:
  - Columns: id (PK), user_id (FK to Users), code_hash (VARCHAR2 hashed), is_used (NUMBER(1)), created_date (TIMESTAMP)

**Oracle DDL Example**:
```sql
ALTER TABLE Users_for_authentication ADD (
  mfa_enabled NUMBER(1) DEFAULT 0,
  mfa_method VARCHAR2(16),
  mfa_secret VARCHAR2(255),
  phone VARCHAR2(20),
  mfa_setup_date TIMESTAMP
);

CREATE TABLE USER_BACKUP_CODES (
  id NUMBER PRIMARY KEY,
  user_id NUMBER NOT NULL,
  code_hash VARCHAR2(255) NOT NULL,
  is_used NUMBER(1) DEFAULT 0,
  created_date TIMESTAMP DEFAULT SYSDATE,
  FOREIGN KEY (user_id) REFERENCES Users_for_authentication(id)
);
```

**Testing**:
- Verify table structure with `DESC Users_for_authentication;`
- Insert test user and verify defaults

#### Step 1b: Users Entity Update
**Files**: `src/main/java/.../Userfolder/Users.java`
**Tasks**:
- Add new fields with Lombok annotations:
  ```java
  @NonNull
  private boolean mfaEnabled = false;
  
  private String mfaMethod;
  
  private String mfaSecret;
  
  @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
  private String phone;
  
  private LocalDateTime mfaSetupDate;
  
  // For backup codes: either transient or separate entity relationship
  @Transient
  private List<String> backupCodes;
  ```
- Keep all Lombok annotations (@Entity, @Builder, @AllArgsConstructor, @NoArgsConstructor, @Getter, @Setter)
- Update UserService.save() to include new fields if provided
- Create separate @Entity BackupCode class if persisting codes as table

**Testing**:
- Build successfully without errors
- Verify Lombok generates getters/setters
- Run existing tests to ensure no regressions

#### Step 1c: Create BackupCode Entity (Optional Separate Entity)
**Files**: `src/main/java/.../Userfolder/BackupCode.java` (new)
**Tasks** (if using separate table):
- Create entity mapped to USER_BACKUP_CODES table
- Fields: id, user (FK to Users), codeHash, isUsed, createdDate
- Repository: BackupCodeRepository with custom queries

**Testing**:
- Entity persists and loads from database
- One-to-many relationship with Users works correctly

---

### Phase 2: MFA Service Core (Est. 1-1.5 days)

#### Step 2a: MfaService Interface & Implementation
**Files**: `src/main/java/.../security/MfaService.java` (new)
**Tasks**:
- Create interface with methods:
  ```java
  String generateTotpSecret();
  String getProvisioningUri(String username, String secret);
  boolean verifyTotpCode(String secret, String code);
  List<String> generateBackupCodes(int count);
  boolean consumeBackupCode(Long userId, String code);
  MfaValidationResult validateMfaState(Users user);
  void enableMfa(Users user, String method);
  void disableMfa(Users user);
  ```
- Implement class: MfaServiceImpl
  - Use TOTP library (e.g., google-authenticator or custom RFC 6238)
  - Hash backup codes using BCrypt
  - Store generated codes, mark consumed codes

**Dependencies to add** (if needed):
- `com.google.guava:guava` (for TOTP base32/base64 utilities)
- Or implement RFC 6238 manually (lightweight option)

**Testing**:
- testGenerateTotpSecret_returnsValidBase32()
- testVerifyTotpCode_withKnownSeed_returnsTrue()
- testBackupCodes_consumeMarksAsUsed()

#### Step 2b: SmsSender Interface (Pluggable)
**Files**: `src/main/java/.../security/sms/SmsSender.java` (new)
**Tasks**:
- Create interface:
  ```java
  void sendMfaCode(String phoneNumber, String code) throws SmsException;
  ```
- Create implementations:
  - `MockSmsSender`: For testing, logs to console
  - `TwilioSmsSender`: Real implementation (future, if Twilio chosen)
- Register as Spring Bean with conditional loading based on `mfa.sms-enabled`

**Testing**:
- testSmsSender_sendCodeSuccessfully()
- testSmsSender_handlesExceptions()

#### Step 2c: Rate Limiting Service
**Files**: `src/main/java/.../security/MfaRateLimiter.java` (new)
**Tasks**:
- Implement rate limiting for:
  - TOTP verification attempts (e.g., max 5 per 15 minutes)
  - SMS sends (e.g., max 3 per hour per phone)
  - Backup code usage (track per user)
- Use Spring's RateLimiter or simple in-memory map (for MVP)
- Log suspicious activity (e.g., 10 failed TOTP attempts)

**Testing**:
- testRateLimit_rejectsAfterMaxAttempts()
- testRateLimit_resetsAfterWindow()

---

### Phase 3: Authentication Flow Integration (Est. 1.5-2 days)

#### Step 3a: MfaFilter (Custom Security Filter)
**Files**: `src/main/java/.../security/MfaFilter.java` (new)
**Tasks**:
- Extend `AbstractAuthenticationProcessingFilter` or create custom filter
- After successful password authentication:
  - Check `user.isMfaEnabled()`
  - If enabled: Redirect to MFA challenge endpoint
  - If disabled: Continue normal flow
- Handle both HTML (browser) and JSON (API) responses
  - Browser: Redirect to /mfa/challenge page with session token
  - API: Return 401 with JSON challenge object

**Testing**:
- testMfaFilter_redirectsWhenMfaEnabled()
- testMfaFilter_continuesWhenMfaDisabled()
- testMfaFilter_supportsBrowserAndApiClients()

#### Step 3b: Add AUTHORIZATION_CODE Grant Type
**Files**: `src/main/java/.../Configuration/ConfigClass.java`
**Tasks**:
- Update `registeredClientRepository()` method:
  ```java
  RegisteredClient client1 = RegisteredClient.withId(...)
    .clientId("client1")
    .clientName("Karthik P N")
    .clientSecret(...)
    .scope(Scopes.READ.name())
    .scope(Scopes.ADMIN.name())
    
    // ADD THESE:
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    
    // Update client settings:
    .clientSettings(ClientSettings.builder()
      .requireAuthorizationConsent(true)
      .build())
    
    // Update token settings:
    .tokenSettings(TokenSettings.builder()
      .accessTokenTimeToLive(Duration.ofHours(1))
      .refreshTokenTimeToLive(Duration.ofDays(7))
      .build())
    .build();
  ```
- Add MfaFilter to the authorization server chain (Order 1)
- Ensure `/mfa/challenge` and `/mfa/confirm` endpoints are permitted without full auth

**Testing**:
- testAuthorizationCodeFlow_withoutMfa_successful()
- testAuthorizationCodeFlow_withMfa_challengeRequired()

#### Step 3c: JWT Token Customizer (Add MFA Claims)
**Files**: `src/main/java/.../security/MfaTokenCustomizer.java` (new)
**Tasks**:
- Implement `OAuth2TokenCustomizer<JwtEncodingContext>`
- Add `mfa_verified` claim based on Authentication
- Add `mfa_level` claim (0 = no MFA, 1 = TOTP verified, etc.)
- Register as Bean in ConfigClass:
  ```java
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(MfaService mfaService) {
    return context -> {
      // Add mfa claims to context.getClaims()
    };
  }
  ```

**Testing**:
- testTokenCustomizer_addsMfaClaims()
- testTokenCustomizer_includesProperValues()

---

### Phase 4: API Endpoints & UI (Est. 1-1.5 days)

#### Step 4a: MfaController (New REST Endpoints)
**Files**: `src/main/java/.../Userfolder/MfaController.java` (new)
**Tasks**:
- Create endpoints:
  - `POST /mfa/setup` (authenticated) → Initiates TOTP setup
  - `POST /mfa/setup/verify` (authenticated) → Verifies code, enables MFA
  - `POST /mfa/disable` (authenticated) → Disables MFA
  - `POST /mfa/challenge` (unauthenticated) → Serves MFA challenge form/JSON
  - `POST /mfa/confirm` (unauthenticated) → Verifies MFA code during login
- Return appropriate DTOs (Setup responses, challenge objects)
- Handle errors with proper HTTP status codes

**Request/Response Examples**:
```json
// POST /mfa/setup
Response:
{
  "secret": "JBSWY3DPEBLW64TMMQ======",
  "provisioning_uri": "otpauth://totp/karthik@example.com?secret=JBSWY3DPEBLW64TMMQ======&issuer=AuthServer",
  "qr_code_url": "data:image/png;base64,..."
}

// POST /mfa/setup/verify
Request:
{
  "totp_code": "123456"
}
Response:
{
  "success": true,
  "message": "MFA enabled successfully",
  "backup_codes": ["code1", "code2", ...]
}

// POST /mfa/confirm
Request:
{
  "username": "john.doe",
  "mfa_code": "123456"
}
Response:
{
  "mfa_token": "xxx.yyy.zzz"
}
```

**Testing**:
- testSetupEndpoint_returnsSecret()
- testVerifyEndpoint_enablesMfa()
- testConfirmEndpoint_validatesCode()

#### Step 4b: MFA Templates (Thymeleaf/HTML)
**Files**: `src/main/resources/templates/mfa/` (new directory)
**Tasks**:
- Create simple HTML templates:
  - `setup.html` — TOTP secret, QR code, manual entry field
  - `setup-verify.html` — TOTP code input during setup
  - `challenge.html` — MFA method selection + code input during login
  - `backup.html` — Backup code usage option
- Style minimally (Bootstrap CDN or inline CSS)
- Handle form submissions to controller endpoints

**Testing**:
- Verify templates render without errors
- Test form submissions work

#### Step 4c: API Responses & Error Handling
**Files**: `src/main/java/.../dto/MfaResponse.java` (new)
**Tasks**:
- Create DTOs for standardized responses:
  - MfaSetupResponse, MfaChallengeResponse, MfaErrorResponse
- Implement global @RestControllerAdvice for exception handling:
  - MfaVerificationException (code validation failure)
  - MfaRateLimitException (too many attempts)
  - MfaConfigurationException (user MFA not properly configured)
- Return proper HTTP status codes (200, 400, 401, 429)

**Testing**:
- testErrorHandler_returnsPropperStatusCode()
- testRateLimitException_returns429()

---

### Phase 5: Comprehensive Testing (Est. 1-1.5 days)

#### Step 5a: Unit Tests
**Files**: `src/test/java/.../security/MfaServiceTest.java` (new)
**Tasks**:
- Fast tests with mocked dependencies
```java
@ExtendWith(MockitoExtension.class)
class MfaServiceTest {
  @InjectMocks private MfaServiceImpl mfaService;
  @Mock private BackupCodeRepository backupCodeRepo;
  
  @Test void testGenerateTotpSecret() { ... }
  @Test void testVerifyTotpValidCode() { ... }
  @Test void testVerifyTotpExpiredCode() { ... }
  @Test void testGenerateBackupCodes() { ... }
}
```

#### Step 5b: Integration Tests
**Files**: `src/test/java/.../MfaAuthenticationIntegrationTest.java` (new)
**Tasks**:
- Use `@SpringBootTest` with Oracle test instance or H2
```java
@SpringBootTest
class MfaAuthenticationIntegrationTest {
  @Test void testUserWithMfaEnabled_receivesChallenge() { ... }
  @Test void testMfaChallenge_withValidCode_succeeds() { ... }
  @Test void testBackupCode_actsAsRecovery() { ... }
}
```

#### Step 5c: OAuth2 Flow Tests
**Files**: `src/test/java/.../AuthorizationCodeFlowTest.java` (new)
**Tasks**:
- Test full authorization code flow with MFA
- Verify JWT tokens contain MFA claims
- Test refresh token behavior

#### Step 5d: Endpoint Tests
**Files**: `src/test/java/.../MfaControllerTest.java` (new)
**Tasks**:
- MockMvc tests for each endpoint
- Verify request/response structure
- Test error scenarios

---

### Phase 6: RSA Key Persistence & Hardening (Est. 1 day)

#### Step 6a: RSA Key Persistence
**Files**: `src/main/java/.../security/RsaKeyManager.java` (new)
**Tasks**:
- Implement key persistence (choose one):
  - Option A: Database (RSA_KEYS table)
  - Option B: Encrypted file (PKCS#12 or PEM format)
  - Option C: Secrets manager (AWS Secrets Manager, HashiCorp Vault)
- Modify ConfigClass to load existing keys instead of generating on startup
- Add key rotation mechanism (future enhancement)

**Testing**:
- testKeyPersistence_loadsExistingKeys()
- testKeyPersistence_generatesNewIfMissing()

#### Step 6b: SecurityConfiguration Review
**Files**: `src/main/java/.../Configuration/ConfigClass.java`
**Tasks**:
- Review and document CSRF settings
- Verify CORS configuration if needed
- Add request logging for security audits
- Document all security endpoints and their accessibility

#### Step 6c: Documentation
**Files**: Update AGENTS.md with MFA section
**Tasks**:
- Document MFA architecture
- Provide configuration examples
- Include troubleshooting guide


````
This is the description of what the code block changes:
<changeDescription>
Appending comprehensive testing strategy, milestones, acceptance criteria, security guidelines, troubleshooting, and developer quick reference sections to complete the plan.
</changeDescription>

This is the code block that represents the suggested code change:
````markdown

---

## Testing Strategy

### Unit Tests (Fast Execution - Mocked)
| Test Class | Test Methods | Purpose | Dependencies |
|-----------|--------------|---------|--------------|
| MfaServiceTest | 8-10 tests | TOTP/backup code logic | Mockito |
| MfaRateLimiterTest | 4-5 tests | Rate limiting enforcement | Mockito |
| MfaTokenCustomizerTest | 3-4 tests | JWT claim injection | Mockito |
| UserServiceTest (extended) | 2 new tests | Password encoding, authority mapping | None (already exists) |

### Integration Tests (Slower - Real Components)
| Test Class | Scenarios | Purpose | Test Data |
|-----------|-----------|---------|-----------|
| MfaAuthenticationIntegrationTest | 5-6 scenarios | End-to-end MFA flows | Test users + Oracle/H2 DB |
| AuthorizationCodeFlowIntegrationTest | 4-5 scenarios | OAuth2 auth_code grant with/without MFA | RegisteredClient |
| MfaControllerIntegrationTest | Endpoint validation | REST endpoint behavior | MockMvc |

### Test Environment Configuration
- **Local**: Oracle test instance or H2 in-memory DB via `@TestPropertySource`
- **CI/CD**: H2 in-memory or Testcontainers Oracle
- **Mocking Strategy**: All external integrations (SMS, audit logs) use mock implementations
- **Data Setup**: SQL scripts for test users with/without MFA

### Test Execution Commands
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests MfaServiceTest

# Run with coverage
./gradlew test --sonar

# Run integration tests only
./gradlew test --tests '*IntegrationTest'
```

---

## Implementation Milestones & Timeline

| Milestone | Description | Est. Hours | Dependencies |
|-----------|-------------|-----------|--------------|
| **Phase 1** | Database & Entity Layer | 4-6 | None |
| **Phase 2** | MFA Service Core | 6-8 | Phase 1 |
| **Phase 3** | Authentication Integration | 8-10 | Phase 1, 2 |
| **Phase 4** | Endpoints & UI | 6-8 | Phase 1, 2, 3 |
| **Phase 5** | Comprehensive Testing | 6-8 | All phases |
| **Phase 6** | RSA Persistence & Hardening | 4-6 | All phases |
| **Phase 7** | Performance & Documentation | 4-6 | All phases |
| **TOTAL** | Full MFA Implementation | 38-52 hours (~5-7 dev days) | - |

### Completion Order Recommendations
1. Start Phase 1 (DB/Entity) — foundational, no blockers
2. Run Phase 2 & 3 in parallel (Service + Integration)
3. Complete Phase 4 (Endpoints)
4. Execute Phase 5 (Testing) throughout
5. Phase 6 (Persistence) can start after Phase 3
6. Phase 7 (Polish) last

---

## Acceptance Criteria

### Functional Requirements
- [x] Spring Boot 4.0.6 with Java 21 (baseline)
- [ ] Users can register via POST /saveuser (existing, no changes)
- [ ] Users can enable TOTP MFA via POST /mfa/setup
- [ ] Users can verify TOTP code and enable MFA
- [ ] Users can disable MFA (authenticated)
- [ ] During login, users with MFA enabled receive MFA challenge
- [ ] Valid TOTP/backup code completes MFA verification
- [ ] Invalid TOTP is rejected with appropriate error
- [ ] Backup codes work as one-time recovery mechanism
- [ ] JWT contains `mfa_verified` claim after successful MFA

### Non-Functional Requirements
- [ ] Unit tests: >90% code coverage for MFA components
- [ ] Integration tests: All major flows covered
- [ ] MFA verification completes in <500ms average
- [ ] Rate limiting prevents brute force (5 attempts/15 min)
- [ ] RSA keys persist across restarts
- [ ] API responses follow consistent JSON schema
- [ ] Proper error messages for all failure scenarios
- [ ] Backward compatibility: Non-MFA users unaffected

### Security Requirements
- [ ] TOTP secrets stored securely (encrypted at rest, hashed in transit)
- [ ] Backup codes hashed before storage
- [ ] Rate limiting prevents MFA enumeration
- [ ] Failed attempts logged for audit
- [ ] JWT claims properly signed/validated
- [ ] No MFA bypass via direct token requests
- [ ] Phone numbers validated before SMS send

### Documentation Required
- [ ] Updated AGENTS.md with MFA architecture section
- [ ] Configuration guide (application.properties additions)
- [ ] API documentation (endpoints, request/response schemas)
- [ ] Troubleshooting guide (common MFA issues)
- [ ] Deployment guide (RSA key setup, database migrations)

---

## Security, Privacy & Operational Considerations

### Data Protection
- **TOTP Secrets**: Encrypt in database, never log or expose in responses
- **Backup Codes**: Hash using BCrypt before storage; compare hashes during verification
- **Phone Numbers**: Validate format, store encrypted, never send in cleartext logs
- **MFA Tokens**: Mark as sensitive; omit from generic logs

### Rate Limiting & Brute Force Prevention
- **TOTP Attempts**: Max 5 failures per 15 minutes per user
- **SMS Sends**: Max 3 per hour per phone number
- **Backup Code Usage**: Max 1 per code (already enforced by is_used flag)
- **Action on Limit**: Clear lock after time window, notify user

### Audit & Logging
- **Successful MFA Setup**: Log with user_id, timestamp, method
- **Failed MFA Attempts**: Log with username, attempt time, method (rate limit context)
- **MFA Disablement**: Log with user_id, reason (for compliance)
- **Sensitive Info**: Never log TOTP secrets, phone numbers, codes
- **Retention**: Keep audit logs for 90+ days for compliance

### Key Management
- **RSA Key Rotation**: Document manual rotation process (future enhancement)
- **Key Backup**: Include RSA keys in disaster recovery plan
- **Key Access**: Restrict to OAuth2 server process only
- **Production Deployment**: Use external secrets manager (AWS Secrets Manager, Vault)

### Compliance Considerations
- **TOTP Algorithm**: RFC 6238 compliant (30-second windows)
- **Backup Codes**: Entropy ≥128 bits per code
- **Password Hashing**: BCrypt (already configured)
- **GDPR**: Support user data export (includes MFA status)
- **Auditability**: Full MFA flow traceable via logs

### Testing Security
- **No Real SMS**: Use mock SmsSender in tests
- **Test Credentials**: Separate from production credentials
- **Sensitive Test Data**: Mark test users as such
- **Cleanup**: Delete test MFA data after test suite

---

## Known Limitations & Future Enhancements

### Current Limitations (MVP)
- In-memory RegisteredClient repository (not scalable to multiple clients)
- No SMS OTP implementation in MVP (TOTP only)
- RSA keys regenerated on startup (not persistent)
- No key rotation mechanism
- Simple rate limiting (in-memory, not distributed)
- No email OTP support
- No hardware key (FIDO2/U2F) support

### Future Enhancements (Post-MVP)
- [ ] Implement database-backed RegisteredClientRepository
- [ ] Add SMS OTP via Twilio/AWS SNS integration
- [ ] Persistent RSA key storage with rotation
- [ ] Distributed rate limiting (Redis-backed)
- [ ] Email OTP as additional MFA method
- [ ] WebAuthn / FIDO2 hardware key support
- [ ] Adaptive authentication (risk-based MFA)
- [ ] Passwordless authentication (FIDO2-only)
- [ ] Multi-device MFA enrollment
- [ ] MFA recovery account links

---

## Common Pitfalls & Troubleshooting

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| TOTP verification always fails | Clock skew or wrong secret | Verify system time, test with known seed |
| MFA filter not triggered | Order wrong or not in chain | Check @Order on filter, verify configuration |
| JWT doesn't include mfa_verified | Token customizer not registered | Verify Bean is created, check method invocation |
| SMS not sent | SmsSender not configured or mock | Add credentials to application.properties |
| Rate limit too strict | Window too short or limit too low | Adjust config, test with actual users |
| Backup codes can be reused | Not properly marked as consumed | Verify BackupCode.is_used flag set |

### Debugging Commands
```bash
# Enable debug logging for authorization server
logging.level.org.springframework.security.oauth2.server.authorization=DEBUG

# Enable debug logging for MFA components
logging.level.com.karthikProjects.AuthServer.security=DEBUG

# Check TOTP verification with known seed
export TOTP_SECRET="JBSWY3DPEBLW64TMMQ======"
# Use TOTP calculator: https://totp.dweems.com/

# Test JWT decoding
import jwt
jwt.decode(token, options={"verify_signature": False})
```

---

## Developer Quick Reference

### Build & Run Commands
```powershell
# Clean build
./gradlew clean build

# Run locally (starts on port 9000)
./gradlew bootRun

# Run tests only
./gradlew test

# Run specific test
./gradlew test --tests MfaServiceTest

# Build JAR
./gradlew build
java -jar build/libs/AuthServer-2.1.jar

# Run with Docker
docker build --build-arg JAR_VERSION=2.1 -t authserver:2.1 .
docker run -p 9000:9000 authserver:2.1
```

### Database Operations
```sql
-- Check Users_for_authentication structure
DESC Users_for_authentication;

-- Insert test user without MFA
INSERT INTO Users_for_authentication (id, username, password, mail, roles, mfa_enabled)
VALUES (seq_users.NEXTVAL, 'testuser', '{BCrypt_hash}', 'test@example.com', 'READ', 0);

-- Add MFA to user
UPDATE Users_for_authentication 
SET mfa_enabled = 1, mfa_method = 'TOTP', mfa_secret = '{secret}'
WHERE username = 'testuser';

-- Check backup codes
SELECT * FROM USER_BACKUP_CODES WHERE user_id = ?;
```

### API Testing Examples
```bash
# Register user
curl -X POST http://localhost:9000/saveuser \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "password": "SecurePass123!",
    "mail": "john@example.com",
    "roles": ["READ"]
  }'

# Setup MFA (must be authenticated)
curl -X POST http://localhost:9000/mfa/setup \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json"

# Verify TOTP code
curl -X POST http://localhost:9000/mfa/setup/verify \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"totp_code": "123456"}'

# Challenge during login
curl -X POST http://localhost:9000/mfa/challenge \
  -H "Content-Type: application/json" \
  -d '{"username": "john.doe", "code": "123456"}'
```

### Files to Modify (Checklist)
- [ ] src/main/java/.../Userfolder/Users.java — Add MFA fields
- [ ] src/main/java/.../UserService.java — Update save() for new fields (optional)
- [x] src/main/java/.../Configuration/ConfigClass.java — Add auth_code grant (currently has TODO)
- [ ] src/main/resources/application.properties — Add MFA configuration
- [ ] src/main/resources/templates/mfa/ — Create MFA templates (new directory)
- [ ] src/main/java/.../security/MfaService.java — Create MFA service (new)
- [ ] src/main/java/.../security/MfaFilter.java — Create auth filter (new)
- [ ] src/main/java/.../Userfolder/MfaController.java — Create endpoints (new)
- [ ] src/test/java/...MfaServiceTest.java — Create unit tests (new)
- [ ] src/test/java/.../MfaAuthenticationIntegrationTest.java — Create integration tests (new)

### Git Workflow
```bash
# Create feature branch
git checkout -b feature/mfa-implementation

# Make incremental commits for each phase
git add src/main/java/.../Users.java
git commit -m "Phase 1: Add MFA fields to Users entity"

# Rebase before merge
git rebase main
git push origin feature/mfa-implementation

# Create Pull Request for code review
```

---

## Appendix: Configuration Templates

### application.properties - MFA Configuration
```properties
# MFA Settings
mfa.enabled=true
mfa.totp.enabled=true
mfa.sms.enabled=false
mfa.backup-codes-enabled=true
mfa.backup-codes-count=10

# Rate Limiting
mfa.rate-limit.totp-attempts=5
mfa.rate-limit.totp-window-minutes=15
mfa.rate-limit.sms-sends=3
mfa.rate-limit.sms-window-hours=1

# TOTP Configuration
mfa.totp.issuer=JWTAuthServer
mfa.totp.window-size=1

# SMS Configuration (if enabled)
# twilio.account-sid=${TWILIO_ACCOUNT_SID:}
# twilio.auth-token=${TWILIO_AUTH_TOKEN:}
# twilio.from-number=${TWILIO_FROM_NUMBER:}

# JWT Claims
jwt.mfa-claim-name=mfa_verified
jwt.mfa-level-claim-enabled=true
```

### build.gradle - Potential MFA Dependencies
```gradle
// Add if implementing TOTP with library
implementation 'com.google.guava:guava:33.0.0-jre'

// Add if using Twilio for SMS
implementation 'com.twilio.sdk:twilio:9.2.3'

// Add if using Redis for distributed rate limiting
implementation 'org.springframework.boot:spring-boot-starter-data-redis'

// Add for QR code generation (TOTP provisioning)
implementation 'com.google.zxing:core:3.5.3'
implementation 'com.google.zxing:javase:3.5.3'
```

---

## Next Steps

**Immediate Actions**:
1. Review this plan document and gather feedback
2. Notify team of MFA implementation timeline (~5-7 dev days)
3. Ensure Oracle test instance is available for Phase 1
4. Create feature branch: `feature/mfa-implementation`

**Phase 1 Kickoff**:
1. Prepare database migration script (Oracle DDL)
2. Create Users.java patch for new fields
3. Write and run migration on test DB
4. Build project, verify no compilation errors
5. Run existing tests to establish baseline

---

**Document Status**: ✓ Complete | **Last Updated**: June 19, 2026 | **Version**: 2.1
