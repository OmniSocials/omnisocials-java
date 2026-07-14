package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 5xx: the API failed on its side. Retried automatically before being thrown. */
public class ServerException extends ApiException {

  public ServerException(int status, String code, String message, JsonNode body) {
    super(status, code, message, body);
  }
}
