package com.karthikProjects.AuthServer.Userfolder;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(indexes =
{@jakarta.persistence.Index(name = "oauth2_registered_client_client_id_index", columnList = "client_id", unique = true)})
public class OAuth2RegisteredClient {
    @Id
    String id;
    @Column(name = "client_id",nullable = false,unique = true)
    String clientId;
    @Column(name = "client_secret")
    String clientSecret;
    @Column(name = "client_secret_expires_at")
    String ClientSecretExpiresAt;
    @Column(name = "client_name")
    String clientName;
    @Column(name = "client_authentication_methods")
    String clientAuthenticationMethods;
    @Column(name = "authorization_grant_types")
    String authorizationGrantTypes;
    @Column(name = "post_logout_redirect_uris")
    String postLogoutRedirectUris;
    @Column(name = "redirect_uris")
    String redirectUris;
    @Column(name = "scopes")
    String Scopes;
    @Column(name = "client_settings",length =  2048)
    String clientSettings;
    @Column(name = "token_settings",length =  2048)
    String tokenSettings;
    @Column(name = "client_id_issued_at")
    Date clientIdIssuedAt;


}
