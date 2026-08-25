package com.karthikProjects.AuthServer.Configuration;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TokenServiceMailTest {

    @Test
    public void handle_sendsEmail_whenMailSenderAvailable() throws Exception {
        TokenServiceClass handler = new TokenServiceClass();

        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // inject mailSender and fromAddress via reflection
        Field f = TokenServiceClass.class.getDeclaredField("emailSender");
        f.setAccessible(true);
        f.set(handler, mailSender);

        Field f2 = TokenServiceClass.class.getDeclaredField("fromAddress");
        f2.setAccessible(true);
        f2.set(handler, "no-reply@test.local");

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

        // verify email creation and send were attempted
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);

        String redirected = response.getRedirectedUrl();
        assertNotNull(redirected, "Response should have been redirected by the handler");
        assertTrue(redirected.contains("/ott/sent"), "Redirect location should contain /ott/sent");
    }

    @Test
    public void handle_skipsEmail_whenMailSenderNull() throws IOException, ServletException {
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

        // Do not inject JavaMailSender — default null
        handler.handle(request, response, token);

        String redirected = response.getRedirectedUrl();
        assertNotNull(redirected, "Response should have been redirected by the handler even when mailSender is null");
        assertTrue(redirected.contains("/ott/sent"));
    }
}

