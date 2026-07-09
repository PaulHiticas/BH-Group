package com.bhgroup.pms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        boolean success,
        String errorCode,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp,
        String path
) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(String errorCode, String message, String path) {
        return new ApiError(false, errorCode, message, null, Instant.now(), path);
    }

    public static ApiError of(String errorCode, String message, List<FieldError> fieldErrors, String path) {
        return new ApiError(false, errorCode, message, fieldErrors, Instant.now(), path);
    }
}
