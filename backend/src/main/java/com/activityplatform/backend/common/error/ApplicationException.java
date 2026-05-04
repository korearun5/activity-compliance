package com.activityplatform.backend.common.error;

import org.springframework.http.HttpStatus;

public class ApplicationException extends RuntimeException {
  private final ErrorCode errorCode;
  private final HttpStatus status;

  public ApplicationException(ErrorCode errorCode, String message, HttpStatus status) {
    super(message);
    this.errorCode = errorCode;
    this.status = status;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }

  public HttpStatus status() {
    return status;
  }
}

