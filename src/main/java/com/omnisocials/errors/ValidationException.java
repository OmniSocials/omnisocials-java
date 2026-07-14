package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 400 or 422: the request was rejected (for example code "validation_error"). */
public class ValidationException extends ApiException {

  public ValidationException(int status, String code, String message, JsonNode body) {
    super(status, code, message, body);
  }
}
