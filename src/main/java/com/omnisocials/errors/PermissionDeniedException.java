package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 403 Forbidden: the API key lacks a required scope (code "insufficient_scope"). */
public class PermissionDeniedException extends ApiException {

  public PermissionDeniedException(int status, String code, String message, JsonNode body) {
    super(status, code, message, body);
  }
}
