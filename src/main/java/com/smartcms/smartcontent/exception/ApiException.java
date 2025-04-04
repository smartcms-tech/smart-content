package com.smartcms.smartcontent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String responseBody;

    // For cases with no response body
    public ApiException(String message, HttpStatus statusCode) {
        super(message);
        this.status = statusCode;
        this.responseBody = null;
    }

    // For detailed error responses
    public ApiException(String message, HttpStatus statusCode, String responseBody) {
        super(formatMessage(message, statusCode, responseBody));
        this.status = statusCode;
        this.responseBody = responseBody;
    }

    private static String formatMessage(String baseMessage, HttpStatus statusCode, String body) {
        return String.format("%s | Status: %d | Body: %s",
                baseMessage, statusCode.value(), body);
    }
}