package com.bhstays.pms.dto.ical;

import com.bhstays.pms.domain.ReservationSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record IcalImportFeedCreateRequest(

        @NotNull(message = "Source is required")
        ReservationSource source,

        @NotBlank(message = "Feed URL is required")
        @URL(message = "Feed URL must be a valid URL")
        String feedUrl
) {
}
