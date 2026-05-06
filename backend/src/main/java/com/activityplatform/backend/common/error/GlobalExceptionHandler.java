package com.activityplatform.backend.common.error;

import com.activityplatform.backend.common.api.ApiError;
import com.activityplatform.backend.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  ResponseEntity<ApiResponse<Void>> handleApplicationException(ApplicationException exception) {
    log.warn(
        "Application exception code={} message={}",
        exception.errorCode(),
        exception.getMessage()
    );
    return error(exception.status(), exception.errorCode(), exception.getMessage(), Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception
  ) {
    Map<String, List<String>> details = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error ->
        details.computeIfAbsent(error.getField(), ignored -> new ArrayList<>())
            .add(error.getDefaultMessage())
    );

    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED,
        "Request validation failed.",
        details
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception
  ) {
    Map<String, List<String>> details = new LinkedHashMap<>();
    exception.getConstraintViolations().forEach(violation ->
        details.computeIfAbsent(violation.getPropertyPath().toString(), ignored -> new ArrayList<>())
            .add(violation.getMessage())
    );

    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED,
        "Request validation failed.",
        details
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
    return error(
        HttpStatus.FORBIDDEN,
        ErrorCode.ACCESS_DENIED,
        "You do not have permission to perform this action.",
        Map.of()
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception
  ) {
    return error(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED,
        "Request body is invalid.",
        Map.of()
    );
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled backend exception", exception);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        "Something went wrong.",
        Map.of()
    );
  }

  private ResponseEntity<ApiResponse<Void>> error(
      HttpStatus status,
      ErrorCode code,
      String message,
      Map<String, List<String>> details
  ) {
    ApiError error = new ApiError(code.name(), message, details, MDC.get("requestId"));
    return ResponseEntity.status(status).body(ApiResponse.failure(error));
  }
}
