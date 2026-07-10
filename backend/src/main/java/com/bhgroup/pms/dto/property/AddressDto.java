package com.bhgroup.pms.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.bhgroup.pms.domain.Address;
public record AddressDto(

        @NotBlank(message = "Address line is required")
        @Size(max = 255)
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String county,

        @Size(max = 20)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        Double latitude,

        Double longitude
) {
}
