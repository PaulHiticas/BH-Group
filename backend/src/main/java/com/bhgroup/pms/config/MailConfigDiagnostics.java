package com.bhgroup.pms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Component;

/**
 * Logs a clear, unmissable warning at startup when SMTP still looks like
 * the dev/placeholder defaults (application.yml's spring.mail.* fallbacks
 * are host=localhost, blank username/password), so it's obvious in the
 * startup log whether real email delivery is configured - rather than
 * discovering it later as a silent "email was logged as failed" entry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailConfigDiagnostics implements CommandLineRunner {

    private final MailProperties mailProperties;

    @Override
    public void run(String... args) {
        String host = mailProperties.getHost();
        String username = mailProperties.getUsername();
        String password = mailProperties.getPassword();

        boolean looksUnconfigured = host == null
                || host.isBlank()
                || "localhost".equalsIgnoreCase(host)
                || username == null || username.isBlank()
                || password == null || password.isBlank()
                || "replace-me".equalsIgnoreCase(username)
                || "replace-me".equalsIgnoreCase(password);

        if (looksUnconfigured) {
            log.warn("SMTP neconfigurat (MAIL_HOST={}) - emailurile NU se trimit efectiv, doar se logheaza "
                    + "esecul (vezi EmailDispatcher). Seteaza MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD reale "
                    + "(vezi .env.example) pentru a trimite email-uri in productie.", host);
        } else {
            log.info("SMTP configurat: host={}", host);
        }
    }
}
