package com.smartcms.smartcontent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.message = message;
    }

    public ApiException(HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
        this.message = cause.getMessage();
    }

    public ApiException(String message, HttpStatusCode statusCode, String errorBody) {
        super(message);
        this.status = HttpStatus.resolve(statusCode.value());
        this.message = message + ": " + errorBody;
    }
}
