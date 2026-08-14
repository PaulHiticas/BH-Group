package com.bhgroup.pms.dto.lead;

import com.bhgroup.pms.domain.LeadType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadCreateRequest(

        @NotBlank(message = "Numele este obligatoriu")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Emailul este obligatoriu")
        @Email(message = "Adresă de email invalidă")
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        @Size(max = 100)
        String city,

        @Size(max = 2000)
        String message,

        LeadType leadType,

        @Min(value = 0, message = "Numărul de dormitoare nu poate fi negativ")
        @Max(value = 20, message = "Numărul de dormitoare este prea mare")
        Integer bedrooms,

        @AssertTrue(message = "Este necesar consimțământul pentru a fi contactat")
        boolean consentGiven,

        @Size(max = 255)
        String utmSource,

        @Size(max = 255)
        String utmMedium,

        @Size(max = 255)
        String utmCampaign,

        /**
         * Honeypot: a field real users never see or fill (hidden via CSS in
         * the form). A non-blank value here means the submission is
         * automated - it's silently accepted (so the bot doesn't learn it
         * was caught) but never persisted or notified.
         */
        String website
) {
}
