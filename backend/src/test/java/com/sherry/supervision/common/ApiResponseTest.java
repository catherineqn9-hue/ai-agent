package com.sherry.supervision.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void shouldBuildSuccessResponseWithBusinessCodeAndTraceId() {
        ApiResponse<Map<String, String>> response = ApiResponse.ok(Map.of("status", "ok"));

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo(BusinessCode.SUCCESS.code());
        assertThat(response.message()).isEqualTo("ok");
        assertThat(response.data()).containsEntry("status", "ok");
        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    void shouldBuildFailureResponseWithBusinessCodeAndTraceId() {
        ApiResponse<Void> response = ApiResponse.fail(BusinessCode.RESOURCE_NOT_FOUND, "事项不存在");

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo(BusinessCode.RESOURCE_NOT_FOUND.code());
        assertThat(response.message()).isEqualTo("事项不存在");
        assertThat(response.data()).isNull();
        assertThat(response.traceId()).isNotBlank();
    }
}
