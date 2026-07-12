package com.bhgroup.pms.service;

import com.bhgroup.pms.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final AppProperties appProperties;

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String rawToken, long expirationMinutes) {
        Context context = new Context();
        context.setVariable("appName", appProperties.getName());
        context.setVariable("firstName", firstName);
        context.setVariable("resetUrl", appProperties.getBaseUrl() + "/reset-password?token=" + rawToken);
        context.setVariable("expirationMinutes", expirationMinutes);

        send(toEmail, "Resetare parolă - " + appProperties.getName(), "email/password-reset-email", context);
    }

    @Async
    public void sendBookingConfirmationEmail(String toEmail, String firstName, String propertyName,
                                              String checkInDate, String checkOutDate, String managementToken) {
        Context context = new Context();
        context.setVariable("appName", appProperties.getName());
        context.setVariable("firstName", firstName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("checkInDate", checkInDate);
        context.setVariable("checkOutDate", checkOutDate);
        context.setVariable("manageUrl", appProperties.getBaseUrl() + "/manage-booking/" + managementToken);

        send(toEmail, "Cererea ta de rezervare - " + appProperties.getName(), "email/booking-confirmation-email", context);
    }

    @Async
    public void sendCheckinInstructionsEmail(String toEmail, String firstName, String propertyName,
                                              String checkInDate, String checkInTime, String address,
                                              String accessCode, String managementToken) {
        Context context = new Context();
        context.setVariable("appName", appProperties.getName());
        context.setVariable("firstName", firstName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("checkInDate", checkInDate);
        context.setVariable("checkInTime", checkInTime);
        context.setVariable("address", address);
        context.setVariable("accessCode", accessCode);
        context.setVariable("manageUrl", appProperties.getBaseUrl() + "/manage-booking/" + managementToken);

        send(toEmail, "Instrucțiuni de check-in - " + appProperties.getName(),
                "email/checkin-instructions-email", context);
    }

    @Async
    public void sendMaintenanceAlertEmail(String toEmail, String firstName, String propertyName,
                                           String ticketTitle, String ticketDescription) {
        Context context = new Context();
        context.setVariable("appName", appProperties.getName());
        context.setVariable("firstName", firstName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("ticketTitle", ticketTitle);
        context.setVariable("ticketDescription", ticketDescription);

        send(toEmail, "Problemă critică la " + propertyName + " - " + appProperties.getName(),
                "email/maintenance-alert-email", context);
    }

    @Async
    public void sendNewMessageEmail(String toEmail, String firstName, String propertyName,
                                     String messageBody, String managementToken) {
        Context context = new Context();
        context.setVariable("appName", appProperties.getName());
        context.setVariable("firstName", firstName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("messageBody", messageBody);
        context.setVariable("manageUrl", appProperties.getBaseUrl() + "/manage-booking/" + managementToken);

        send(toEmail, "Mesaj nou despre rezervarea ta - " + appProperties.getName(),
                "email/new-message-email", context);
    }

    private void send(String toEmail, String subject, String template, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(appProperties.getMail().getFrom());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send email to {} using template {}", toEmail, template, ex);
        }
    }
}
