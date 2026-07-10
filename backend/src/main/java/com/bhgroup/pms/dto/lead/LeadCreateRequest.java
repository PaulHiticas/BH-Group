package com.bhgroup.pms.dto.lead;

import jakarta.validation.constraints.Email;
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
        String message
) {
}
