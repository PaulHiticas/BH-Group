package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhstays.pms.config.AppProperties;
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
        appProperties.getMail().setFrom("no-reply@bhstays.ro");
        emailDispatcher = new EmailDispatcher(mailSender, templateEngine, appProperties);
    }

    @Test
    void dispatch_sendsTheRenderedEmail_whenMailSenderWorks() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");

        emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context());

        verify(mailSender).send(any(MimeMessage.class));
    }

    /**
     * From is the no-reply address on the DKIM-signed domain, so a guest who
     * hits Reply has to be steered to a mailbox somebody actually reads.
     */
    @Test
    void dispatch_setsReplyToTheConfiguredMailbox() throws Exception {
        appProperties.getMail().setReplyTo("bhstaysinfo@gmail.com");
        MimeMessage message = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");

        emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context());

        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@bhstays.ro");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("bhstaysinfo@gmail.com");
    }

    @Test
    void dispatch_leavesReplyToUnset_whenNotConfigured() throws Exception {
        appProperties.getMail().setReplyTo(null);
        MimeMessage message = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>ok</html>");

        emailDispatcher.dispatch("guest@example.com", "Subiect", "email/password-reset-email", new Context());

        // With no Reply-To header, getReplyTo() falls back to the From address.
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("no-reply@bhstays.ro");
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
