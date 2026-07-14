package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/** 429 Too Many Requests: the 100 requests/minute rate limit was exceeded. */
public class RateLimitException extends ApiException {

  private final Long retryAfter;

  public RateLimitException(
      int status, String code, String message, JsonNode body, Long retryAfter) {
    super(status, code, message, body);
    this.retryAfter = retryAfter;
  }

  /**
   * Seconds to wait before retrying, taken from the {@code Retry-After}
   * response header, or {@code null} when the header was absent.
   */
  public Long getRetryAfter() {
    return retryAfter;
  }
}
