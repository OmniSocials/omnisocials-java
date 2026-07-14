package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 404 Not Found: the requested resource does not exist in this workspace. */
public class NotFoundException extends ApiException {

  public NotFoundException(int status, String code, String message, JsonNode body) {
    super(status, code, message, body);
  }
}
