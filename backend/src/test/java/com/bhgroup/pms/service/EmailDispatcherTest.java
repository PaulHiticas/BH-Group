package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class EmailDispatcherTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private SpringTemplateEngine templateEngine;

    private AppProperties appProperties;
    private EmailDispatcher emailDispatcher;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getMail().setFrom("no-reply@bhgroup.io");
        emailDispatcher = new EmailDispatcher(mailSender, templateEngine, appProperties);
    }

    @Test
    void dispatch_sendsTheRenderedEmail_whenMailSenderWorks() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");

        emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void dispatch_swallowsAnSmtpFailure_doesNotThrow() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");
        doThrow(new MailSendException("SMTP connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() ->
                emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context())
        ).doesNotThrowAnyException();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void dispatch_swallowsATemplateRenderingFailure_doesNotThrow() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template not found"));

        assertThatCode(() ->
                emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context())
        ).doesNotThrowAnyException();
    }
}
