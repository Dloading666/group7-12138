package com.rpa.management.common.exception;

public class BadRequestBusinessException extends BusinessException {

    public BadRequestBusinessException(String message) {
        super(400, message);
    }
}
