package com.horizon.backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "Method not allowed"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "Internal server error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "Invalid type value"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "Access denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "Resource not found"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "Token has expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "Invalid refresh token"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "A005", "Authentication failed"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "Email already exists"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "Invalid password"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "U004", "User account is disabled"),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "V001", "Validation error"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "V002", "Missing required field"),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "V003", "Invalid format");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
