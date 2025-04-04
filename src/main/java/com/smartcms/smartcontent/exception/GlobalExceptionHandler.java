package com.smartcms.smartcontent.exception;

import com.smartcms.smartcommon.exception.ResourceNotFoundException;
import com.smartcms.smartcommon.exception.ServiceLayerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationException(IllegalArgumentException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ClientException.class)
    public ResponseEntity<String> handleClientException(ClientException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ServiceLayerException.class)
    public ResponseEntity<String> handleServiceLayerException(ServiceLayerException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidScheduleTimeException.class)
    public ResponseEntity<String> handleInvalidScheduleTimeException(InvalidScheduleTimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException ex) {
        return new ResponseEntity<>(ex.getMessage(), ex.getStatus());
    }


}
