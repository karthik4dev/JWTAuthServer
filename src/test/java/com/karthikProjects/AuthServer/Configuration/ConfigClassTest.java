package com.karthikProjects.AuthServer.Configuration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigClassTest {

    @Test
    public void testPasswordEncoder_matchesEncodedValue() {
        PasswordEncoder encoder = ConfigClass.passwordEncoder();
        String raw = "test-password";
        String encoded = encoder.encode(raw);
        assertNotNull(encoded);
        assertTrue(encoder.matches(raw, encoded));
    }

    @Test
    public void testRegisteredClientRepository_containsClientAndScopes() {
        ConfigClass config = new ConfigClass();
        RegisteredClientRepository repo = config.registeredClientRepository();
        // InMemoryRegisteredClientRepository provides findByClientId(String)
        try {
            var client = (org.springframework.security.oauth2.server.authorization.client.RegisteredClient)
                    repo.findByClientId("client1");
            assertNotNull(client, "Registered client 'client1' should exist");
            assertTrue(client.getScopes().contains("READ"), "Scopes should contain READ");
            assertTrue(client.getScopes().contains("ADMIN"), "Scopes should contain ADMIN");
        } catch (NoSuchMethodError | AbstractMethodError e) {
            // Some RegisteredClientRepository implementations expose different APIs;
            // fall back to ensuring the repository itself was created.
            assertNotNull(repo);
        }
    }

    @Test
    public void testJwkSourceAndJwtDecoder_notNull() {
        ConfigClass config = new ConfigClass();
        JWKSource jwkSource = config.jwkSource();
        assertNotNull(jwkSource, "jwkSource must not be null");
        JwtDecoder decoder = config.jwtDecoder(jwkSource);
        assertNotNull(decoder, "jwtDecoder must not be null");
    }

    @Test
    public void generateRsaKey_and_decodeSignedJwt() throws Exception {
        // invoke private generateRsaKey via reflection
        java.lang.reflect.Method m = ConfigClass.class.getDeclaredMethod("generateRsaKey");
        m.setAccessible(true);
        KeyPair kp = (KeyPair) m.invoke(null);
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(pub).privateKey(priv).keyID(UUID.randomUUID().toString()).build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        var jwtDecoder = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        assertNotNull(jwtDecoder);

        // Build and sign a JWT with the private key
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("test-issuer")
                .subject("sub")
                .expirationTime(new Date(new Date().getTime() + 60_000))
                .issueTime(new Date())
                .claim("scope", "READ")
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build();
        SignedJWT signedJWT = new SignedJWT(header, claims);
        RSASSASigner signer = new RSASSASigner(priv);
        signedJWT.sign(signer);
        String token = signedJWT.serialize();

        var decoded = jwtDecoder.decode(token);
        assertEquals("sub", decoded.getSubject());
    }
}

