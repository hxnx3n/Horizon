package com.horizon.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Invalid %s: '%s' = '%s'", resourceName, fieldName, fieldValue));
    }
}
