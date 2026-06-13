package com.sherry.supervision.exception;

import com.sherry.supervision.common.BusinessCode;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(BusinessCode.RESOURCE_NOT_FOUND, message);
    }
}
