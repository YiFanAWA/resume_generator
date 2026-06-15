package com.daemonsets.resumeportal.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackageClasses = ApiController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(status).body(ApiErrorResponse.of(message, status.value(), request.getRequestURI()));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<?> handleConcurrentProfileWrite(
            PessimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("Profile is being updated by another request. Please retry shortly.",
                        HttpStatus.CONFLICT.value(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(exception.getMessage(), HttpStatus.BAD_REQUEST.value(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("Internal server error",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), request.getRequestURI()));
    }
}
