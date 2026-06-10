# AGENTS.md - AuthServer Guide for AI Coding Agents

## Project Overview
**JWTAuthServer** is a Spring Boot 4.0.6 OAuth2 Authorization Server using JWT tokens with RSA-2048 encryption. Designed for stateless authentication across microservices with role-based access control (RBAC) via scopes.

**Stack**: Java 21 | Spring Boot 4.0.6 | Spring Security OAuth2 | JPA/Hibernate | Oracle DB | Gradle | Docker

---

## Architecture & Data Flow

### Core Components
- **Users Entity** (`Userfolder/Users.java`): JPA entity (table: `Users_for_authentication`) with username, password, mail, and ArrayList of Scopes (roles). All fields use Lombok annotations (@Entity, @Builder, @AllArgsConstructor, @NoArgsConstructor, @Getter, @Setter)
- **UserController** (`Userfolder/UserController.java`): Single POST endpoint `/saveuser` for user registration (public)
- **UserService** (`Userfolder/UserService.java`): Implements UserDetailsService; bridges database and Spring Security
- **ConfigClass** (`Configuration/ConfigClass.java`): 177 lines of security configuration - **critical for auth flow**

### Project Structure Notes
- **AuthServerApplication.java**: Entry point with @SpringBootApplication annotation
- **Placeholder/Empty Files** (not currently used):
  - `Roles.java`: Empty enum file (consider if distinct from Scopes.java)
  - `UserPrincipal.java`: Empty class stub
  - `RoleIsNotCorrectException.java`: Empty exception class stub

### Authorization Flow
1. Client calls `/saveuser` → UserController → UserService.save()
2. UserService hashes password (BCryptPasswordEncoder) and persists to Oracle
3. For OAuth2 token requests: Spring Security loads user via UserService.loadUserByUsername()
4. ConfigClass generates RSA key pair (2048-bit) for JWT signing
5. Token issued with Scopes enum values as authorities

### Why Two Security Filter Chains?
ConfigClass defines two @Bean chains with @Order:
- **Order(1)**: Authorization server endpoints (JWT issuance, OAuth2, OIDC)
- **Order(2)**: Default form login (handles user auth for browser flows)
Both permit `/saveuser` without authentication. Current setup: RegisteredClient configured with CLIENT_CREDENTIALS and REFRESH_TOKEN grant types. To add authorization_code flow, modify TokenSettings and ClientSettings in registeredClientRepository() bean (line 121-138).

### Database & ORM Specifics
- **Platform**: Oracle with HikariCP pooling (5-20 connections)
- **JPA Mode**: `ddl-auto=update` (auto-creates/updates schema)
- **Show SQL**: Enabled in properties for debugging
- **Custom Query**: UserRepository.findByUsername() used by UserDetailsService

---

## Development Workflows & Build Commands

### Build & Run
```bash
# Gradle build (uses Java 21 toolchain)
./gradlew clean build

# Run locally (starts on port 9000)
./gradlew bootRun

# Build JAR (outputs to build/libs/)
./gradlew build

# Run JAR
java -jar build/libs/AuthServer-2.1.jar
```

### Docker
```bash
# Build image (ARG JAR_VERSION=1.2)
docker build --build-arg JAR_VERSION=2.1 -t authserver:2.1 .

# Run container (exposes 9000)
docker run -p 9000:9000 authserver:2.1
```

### Testing
```bash
# Run all tests (uses JUnit 5 platform)
./gradlew test

# Specific test class
./gradlew test --tests MockitoTestClasses
```

**Test Patterns**:
- `AuthServerApplicationTests`: Context loading test with @SpringBootTest
- `MockitoTestClasses`: Uses @MockitoBean to mock UserRepository
- Tests use @TestPropertySource to load application.properties

---

## Critical Conventions & Patterns

### 1. Lombok Everywhere
All entities use `@Entity @Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter`. This is non-negotiable. When adding fields to Users, add Lombok annotations first.

**Current Users fields**:
- `id`: Long with @GeneratedValue(strategy = GenerationType.SEQUENCE)
- `username`: String with @NonNull and unique constraint
- `password`: String (stored as BCrypt hash)
- `mail`: String with @NonNull (note: field name is `mail`, not `email`) and email validation pattern
- `roles`: ArrayList<Scopes> with @NonNull

**Example for new field**: If extending Users with a `phone` field:
```java
@NonNull
@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
private String phone;
```

### 2. Email Validation Pattern
Users `mail` field uses strict regex (note: the field is named `mail`, not `email` in the Users entity). Modify cautiously:
- Pattern: `^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]{1,51}+@[a-zA-Z0-9.-]+\\.[A-Za-z]{2,3}$`
- Error message: "Email should start and should consist 1,12 alpha-numeric characters(Can also Contain special characters) have @ in between and should end with domain"

### 3. Password Encoding Requirement
ConfigClass.passwordEncoder() returns BCryptPasswordEncoder as static method. **UserService always encodes passwords via ConfigClass.passwordEncoder().encode()** - never store plain text. Verify this in save() method.

### 4. Security Configuration Coupling
ConfigClass strongly couples:
- OAuth2AuthorizationServerConfigurer (RSA JWK setup)
- RegisteredClientRepository (in-memory, single client "client1")
- AuthenticationProvider using UserDetailsService

**Current Setup**: RegisteredClient configured with CLIENT_CREDENTIALS and REFRESH_TOKEN grant types. To add authorization_code flow, modify TokenSettings and ClientSettings in registeredClientRepository() bean (lines 121-138).

### 5. Scopes Enum for Authorities
Scopes is a two-value enum: `READ, ADMIN`. Users.roles is `ArrayList<Scopes>`. UserService.loadUserByUsername() converts to String for Spring Security roles. **When adding new scopes, update Scopes.java and ConfigClass.registeredClientRepository()**.

### 6. Repository Pattern
Only method: `findByUsername(String)` - returns Optional. Used by UserDetailsService and implicitly by UserController (no explicit repo calls, all via UserService).

---

## Key Configuration & Dependencies

**application.properties** (src/main/resources/):
- Server port: 9000
- Oracle thin driver: localhost:1521/xepdb1 (credentials in plaintext - **dev only**)
- Logging: Spring Security at INFO, Hibernate at DEBUG
- JPA: OracleDialect, show_sql=true, ddl-auto=update

**Gradle Dependencies** (build.gradle):
- spring-boot-starter-oauth2-authorization-server
- spring-security-oauth2-jose (JOSE library for JWT)
- org.projectlombok (annotation processing)
- org.mockito for testing
- oracle.jdbc ojdbc11 (runtime only)

**Removed/Commented Code**:
- Lines 29-35 in build.gradle: Old Spring Boot 1.x OAuth2 library versions
- Lines 27-34 in application.properties: OIDC client config commented out

---

## Common Pitfalls & Notes

1. **In-Memory Client Repository**: ConfigClass uses InMemoryRegisteredClientRepository. Not suitable for multi-client systems. Extend RegisteredClientRepository interface for database persistence.

2. **Stateless vs. Session**: /saveuser endpoint doesn't require auth, but token validation requires authenticated session setup. Test with actual OAuth2 flows (authorization_code, client_credentials).

3. **Email Validation**: Regex is strict; update carefully. Test with valid domains (e.g., user@example.com).

4. **RSA Key Generation**: ConfigClass.generateRsaKey() creates new key pair on app startup - every restart invalidates old tokens. Add persistent key storage for production.

5. **CSRF Disabled**: Both filter chains disable CSRF (AbstractHttpConfigurer::disable). OK for stateless API-first design, but revisit if adding session-based features.

6. **Test Database**: Tests inherit application.properties (Oracle connection) - ensure test Oracle instance is running or use @TestPropertySource with H2 for CI/CD.

---

## Emerging Patterns (Features Under Development)

From README checklist (lines 43-48): EUREKA handler integration planned for multi-service authentication. Expect future changes to ConfigClass for service registry.

---

## Where to Start
1. **Understanding user auth**: Read UserService (loads from DB) → ConfigClass (issues JWT)
2. **Adding endpoints**: See UserController (simple POST pattern)
3. **Modifying security rules**: ConfigClass lines 81-84 and 103-105 (permit/authenticated rules)
4. **Adding tests**: Follow MockitoTestClasses pattern (mock repo, test exception flows)

