package com.sherry.supervision.exception;

import com.sherry.supervision.common.BusinessCode;

public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String message) {
        super(BusinessCode.PARAM_INVALID, message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(BusinessCode.PARAM_INVALID, message, cause);
    }
}
