package com.sherry.supervision.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        T data,
        @JsonProperty("trace_id") String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, BusinessCode.SUCCESS.code(), BusinessCode.SUCCESS.defaultMessage(), data, generateTraceId());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, BusinessCode.SUCCESS.code(), message, data, generateTraceId());
    }

    public static ApiResponse<Void> fail(BusinessCode businessCode, String message) {
        return new ApiResponse<>(false, businessCode.code(), message, null, generateTraceId());
    }

    public static ApiResponse<Void> fail(BusinessCode businessCode) {
        return fail(businessCode, businessCode.defaultMessage());
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
