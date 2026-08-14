package com.bhgroup.pms.dto.gdpr;

public record GdprEraseResultResponse(
        int reservationsErased,
        int leadsErased,
        int messagesRedacted,
        int lateCheckoutNotesRedacted
) {
}
