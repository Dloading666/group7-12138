package com.rpa.management.common.exception;

public class ForbiddenBusinessException extends BusinessException {

    public ForbiddenBusinessException(String message) {
        super(403, message);
    }
}
