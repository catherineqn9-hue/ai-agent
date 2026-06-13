package com.sherry.supervision.common;

import com.sherry.supervision.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        BusinessCode code = exception.businessCode();
        return ResponseEntity.status(code.httpStatus())
                .body(ApiResponse.fail(code, exception.getMessage()));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity.status(BusinessCode.PARAM_INVALID.httpStatus())
                .body(ApiResponse.fail(BusinessCode.PARAM_INVALID, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = BusinessCode.PARAM_INVALID.defaultMessage();
        }
        return ResponseEntity.status(BusinessCode.PARAM_INVALID.httpStatus())
                .body(ApiResponse.fail(BusinessCode.PARAM_INVALID, message));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyResult(EmptyResultDataAccessException exception) {
        return ResponseEntity.status(BusinessCode.RESOURCE_NOT_FOUND.httpStatus())
                .body(ApiResponse.fail(BusinessCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(BusinessCode.BUSINESS_CONFLICT.httpStatus())
                .body(ApiResponse.fail(BusinessCode.BUSINESS_CONFLICT, "数据已存在或违反业务唯一约束"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException exception) {
        return ResponseEntity.status(BusinessCode.BUSINESS_CONFLICT.httpStatus())
                .body(ApiResponse.fail(BusinessCode.BUSINESS_CONFLICT, "数据操作失败"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(BusinessCode.SYSTEM_ERROR.httpStatus())
                .body(ApiResponse.fail(BusinessCode.SYSTEM_ERROR));
    }
}
