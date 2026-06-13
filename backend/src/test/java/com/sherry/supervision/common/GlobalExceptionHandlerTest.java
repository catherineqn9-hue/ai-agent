package com.sherry.supervision.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.sherry.supervision.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldTranslateBusinessExceptionToStandardResponse() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(BusinessCode.RESOURCE_NOT_FOUND, "督办事项不存在"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(40400);
        assertThat(response.getBody().message()).isEqualTo("督办事项不存在");
    }
}
