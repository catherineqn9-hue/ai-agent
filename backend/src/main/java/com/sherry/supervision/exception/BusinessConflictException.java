package com.sherry.supervision.exception;

import com.sherry.supervision.common.BusinessCode;

public class BusinessConflictException extends BusinessException {

    public BusinessConflictException(String message) {
        super(BusinessCode.BUSINESS_CONFLICT, message);
    }
}
