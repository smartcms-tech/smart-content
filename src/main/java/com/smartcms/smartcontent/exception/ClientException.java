package com.smartcms.smartcontent.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientException extends RuntimeException {
    private final HttpStatus status;

    public ClientException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ClientException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}

