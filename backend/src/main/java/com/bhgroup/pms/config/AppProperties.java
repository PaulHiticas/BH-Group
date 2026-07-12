package com.bhgroup.pms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String baseUrl;
    private String apiBaseUrl;
    private Mail mail = new Mail();
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Mail {
        private String from;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
        private String issuer;
    }

    @Getter
    @Setter
    public static class Security {
        private long emailVerificationTokenExpirationMinutes;
        private long passwordResetTokenExpirationMinutes;
        private int maxLoginAttempts;
        private long loginLockoutMinutes;
    }

    @Getter
    @Setter
    public static class Storage {
        private String uploadDir;
        private String publicBaseUrl;
    }
}
