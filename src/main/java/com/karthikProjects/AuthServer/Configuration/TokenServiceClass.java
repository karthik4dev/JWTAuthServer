package com.karthikProjects.AuthServer.Configuration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@Configuration
public class TokenServiceClass implements OneTimeTokenGenerationSuccessHandler {

    private final OneTimeTokenGenerationSuccessHandler redirectHandler = new RedirectOneTimeTokenGenerationSuccessHandler("/ott/sent");

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username:no-reply@example.com}")
    private String fromAddress;

    @Override
    public void handle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, OneTimeToken oneTimeToken) throws IOException, ServletException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(URI.create(UrlUtils.buildFullRequestUrl(request)))
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .fragment(null)
                .path("/login/ott")
                .queryParam("token", oneTimeToken.getTokenValue());
        String magicLink = builder.toUriString();

        System.out.println("magicLink: " + magicLink);

        // Send magic link email (best-effort). If no JavaMailSender is configured (e.g. in unit tests), skip sending.
        if (emailSender != null) {
            try {
                MimeMessage message = emailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress);
                // TODO: replace hardcoded recipient with actual user email when available via request/OneTimeToken metadata
                helper.setTo("karthikpnrao.97@gmail.com");
                helper.setSubject("One Time Token");
                helper.setText("Click the following link to login: " + magicLink, true);
                emailSender.send(message);
            } catch (MessagingException e) {
                // Log and continue — email sending should not block the authentication flow
                System.err.println("Failed to send OTT email: " + e.getMessage());
            }
        }

        this.redirectHandler.handle(request, response, oneTimeToken);

    }
}
