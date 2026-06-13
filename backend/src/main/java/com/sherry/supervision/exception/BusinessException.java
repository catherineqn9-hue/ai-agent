package com.sherry.supervision.exception;

import com.sherry.supervision.common.BusinessCode;

public class BusinessException extends RuntimeException {

    private final BusinessCode businessCode;

    public BusinessException(BusinessCode businessCode) {
        super(businessCode.defaultMessage());
        this.businessCode = businessCode;
    }

    public BusinessException(BusinessCode businessCode, String message) {
        super(message);
        this.businessCode = businessCode;
    }

    public BusinessException(BusinessCode businessCode, String message, Throwable cause) {
        super(message, cause);
        this.businessCode = businessCode;
    }

    public BusinessCode businessCode() {
        return businessCode;
    }
}
