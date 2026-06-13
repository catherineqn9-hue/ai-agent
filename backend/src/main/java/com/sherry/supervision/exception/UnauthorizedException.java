package com.sherry.supervision.exception;

import com.sherry.supervision.common.BusinessCode;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(BusinessCode.UNAUTHORIZED, message);
    }
}
