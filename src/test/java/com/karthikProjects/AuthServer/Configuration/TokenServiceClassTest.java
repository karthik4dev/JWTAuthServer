package com.karthikProjects.AuthServer.Configuration;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ott.OneTimeToken;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenServiceClassTest {

    @Test
    public void handle_buildsMagicLink_andRedirects() throws IOException, ServletException {
        TokenServiceClass handler = new TokenServiceClass();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setRequestURI("/some/path");
        request.setContextPath("");

        MockHttpServletResponse response = new MockHttpServletResponse();

        OneTimeToken token = mock(OneTimeToken.class);
        when(token.getTokenValue()).thenReturn("my-ott-token");

        handler.handle(request, response, token);

        String redirected = response.getRedirectedUrl();
        assertNotNull(redirected, "Response should have been redirected by the handler");
        assertTrue(redirected.contains("/ott/sent"), "Redirect location should contain /ott/sent");
    }
}

