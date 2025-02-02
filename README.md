# JWTAuthServer
Creating JWT Authorization Server 

## Need for JWT Authorization Server

A **JWT (JSON Web Token) authorization server** is crucial when you need to manage secure communication between different services, particularly in a **microservices architecture** or for handling authentication and authorization in web applications. Here are some reasons why an authorization server using JWT is necessary:

### 1. **Stateless Authentication**:
   - **JWT** allows you to implement stateless authentication, meaning the server doesn't need to store session information. The token itself contains the necessary data (like user information and expiration date), and is validated via its signature. This reduces server overhead and makes scaling easier.
   
### 2. **Security**:
   - **JWTs** are signed (using a secret or private key) and optionally encrypted. This ensures that the token can’t be tampered with. The server can verify the signature to check the token's integrity before granting access to resources.

### 3. **Single Sign-On (SSO)**:
   - JWT allows you to implement **Single Sign-On** systems, where one login event gives the user access to multiple services without needing to log in separately to each one. The user’s identity is encoded within the JWT and can be used across different microservices or domains.

### 4. **Ease of Integration**:
   - JWT is widely used across many web frameworks, libraries, and APIs. It’s easy to integrate with REST APIs, mobile apps, and even third-party services that support token-based authentication.

### 5. **Role-Based Access Control (RBAC)**:
   - The JWT can contain user roles and permissions, which helps in enforcing **role-based access control**. The authorization server can issue tokens that specify what a user is allowed to do, and each microservice can check these permissions before allowing access.

### 6. **Cross-domain Authentication**:
   - Since JWT is a self-contained token, it is ideal for **cross-domain** or **cross-platform** authentication. The token can be used across different systems without any direct dependency on the server, enabling easy interaction between different apps or services.

### 7. **Reduced Load on the Server**:
   - With JWT, the need for storing user sessions is eliminated. Since the authentication data is embedded directly in the token, the server doesn’t need to query a database or session store for each request, which improves overall performance.

### 8. **Decentralized and Scalable**:
   - In a distributed system, you can scale independently since each service can verify the JWT token by itself without relying on a centralized authentication server for every request.

### 9. **Expiration and Revocation**:
   - JWT tokens have an **expiration time**, ensuring they don’t remain valid indefinitely. This is helpful for limiting the lifespan of access tokens and improving security. Tokens can also be revoked by changing the signing key or using a revocation list, depending on how you set it up.

### 10. **Third-party Authorization (OAuth2)**:
   - When combined with protocols like **OAuth2**, JWT can be used for delegating authentication and authorization. For example, a user can authenticate using their Google or Facebook account, and the authorization server issues a JWT representing the user’s identity.

In essence, the JWT authorization server provides the ability to handle **secure authentication, manage roles and permissions**, and **scale easily** across multiple services, all while maintaining a **stateless** architecture that simplifies operations. It’s highly useful in modern, distributed, and microservices-based applications.

## How to run this Jar ##
 Simply run the java -jar "Jar name" which you have built

## chnges to be done in upcoming editions ##
1. Run it with OpenJDK
2. Run it with Docker
3. Enable Basic Logging for the JWT Resource server for better monitoring
4. Use this server authentication and authorization of other RESTApis present in repo. This can be handled via EUREKA handler.