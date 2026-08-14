package com.bhgroup.pms.dto.latecheckout;

import jakarta.validation.constraints.Size;

public record LateCheckoutRequestCreateRequest(
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String guestNote
) {
}
