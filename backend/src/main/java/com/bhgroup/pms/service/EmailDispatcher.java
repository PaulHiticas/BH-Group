package com.bhgroup.pms.service;

import com.bhgroup.pms.common.PiiMasking;
import com.bhgroup.pms.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Does the actual SMTP I/O for {@link EmailService}, off the caller's thread
 * and never before the caller's transaction (if any) has committed - see
 * EmailService.send() for the transaction-timing decision. Split into its
 * own bean (not a private method on EmailService) specifically so @Async
 * applies: Spring's AOP proxy can't intercept a same-class self-invocation,
 * so EmailService calling its own method directly would silently run
 * synchronously and block on SMTP I/O.
 *
 * <p>Package-private - nothing outside this package should call this
 * directly instead of going through EmailService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class EmailDispatcher {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties appProperties;

    @Async
    void dispatch(String toEmail, String subject, String template, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {} using template {}", PiiMasking.maskEmail(toEmail), template, ex);
        }
    }
}
