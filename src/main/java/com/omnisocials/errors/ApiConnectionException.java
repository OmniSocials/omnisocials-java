package com.omnisocials.errors;

/** Network failure, timeout, or interruption while talking to the API. */
public class ApiConnectionException extends OmniSocialsException {

  public ApiConnectionException(String message) {
    super(message);
  }

  public ApiConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
