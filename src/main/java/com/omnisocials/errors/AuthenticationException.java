package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 401 Unauthorized: missing, malformed, or revoked API key. */
public class AuthenticationException extends ApiException {

  public AuthenticationException(int status, String code, String message, JsonNode body) {
    super(status, code, message, body);
  }
}
