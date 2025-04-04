package com.smartcms.smartcontent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class ApiException extends RuntimeException {
    private Integer statusCode;
    private String message;

    public ApiException(String message) {
        super(message);
        this.message = message;
    }

    public ApiException(Throwable cause) {
        super(cause);
        this.message = cause == null ? null : cause.toString();
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public ApiException(HttpStatus status) {
        this.statusCode = status.value();
    }

    public ApiException(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public ApiException(Throwable cause, HttpStatus status) {
        super(cause);
        this.statusCode = status.value();
        this.message = cause == null ? null : cause.toString();
    }

    public ApiException(Throwable cause, Integer statusCode) {
        super(cause);
        this.statusCode = statusCode;
        this.message = cause == null ? null : cause.toString();
    }

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.statusCode = status.value();
        this.message = message;
    }

    public ApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
    }

    public ApiException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.statusCode = status.value();
        this.message = message;
    }

    public ApiException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.message = message;
    }

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Integer statusCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.statusCode = statusCode;
        this.message = message;
    }

    public ApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, HttpStatus status) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.statusCode = status.value();
        this.message = message;
    }

    public ApiException(String apiCallFailed, HttpStatusCode statusCode, String errorBody) {
        super(apiCallFailed);
        this.statusCode = statusCode.value();
        this.message = apiCallFailed + ": " + errorBody;
    }
}