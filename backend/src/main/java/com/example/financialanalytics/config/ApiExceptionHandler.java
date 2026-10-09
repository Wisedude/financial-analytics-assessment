package com.example.financialanalytics.config;

import com.example.financialanalytics.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String,String>> api(ApiException ex) {
        if (ex.status().is5xxServerError()) {
            log.error("API request failed with status {}", ex.status().value(), ex);
        } else {
            log.warn("API request rejected with status {}: {}", ex.status().value(), ex.getMessage());
        }
        return ResponseEntity.status(ex.status()).body(error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().findFirst().map(e -> e.getField()+": "+e.getDefaultMessage()).orElse("Invalid request");
        log.warn("Request validation failed: {}", message);
        return ResponseEntity.badRequest().body(error(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String,String>> unreadable(HttpMessageNotReadableException ex) {
        log.warn("Request body could not be parsed");
        return ResponseEntity.badRequest().body(error("Invalid request body"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class})
    ResponseEntity<Map<String,String>> invalidParameter(Exception ex) {
        log.warn("Request parameter validation failed: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(error("Invalid request parameter"));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<Map<String,String>> notFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("Resource not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String,String>> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(error("HTTP method not allowed"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> conflict(DataIntegrityViolationException ex) {
        log.warn("Database constraint rejected a request");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("A record with these details already exists"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String,String>> unexpected(Exception ex) {
        log.error("Unhandled API exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("An unexpected error occurred"));
    }

    private Map<String,String> error(String message) {
        String requestId = MDC.get("requestId");
        return requestId == null ? Map.of("message", message)
                : Map.of("message", message, "requestId", requestId);
    }
}
