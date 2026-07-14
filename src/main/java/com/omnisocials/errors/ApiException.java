package com.omnisocials.errors;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A non-2xx response from the OmniSocials API.
 *
 * <p>{@code code} and the exception message are taken from the API error body
 * {@code { "error": { "code", "message" } }} when present.
 */
public class ApiException extends OmniSocialsException {

  private final int status;
  private final String code;
  private final transient JsonNode body;

  public ApiException(int status, String code, String message, JsonNode body) {
    super(message);
    this.status = status;
    this.code = code;
    this.body = body;
  }

  /** HTTP status code of the failed response. */
  public int getStatus() {
    return status;
  }

  /**
   * Machine-readable error code from the API body (for example
   * {@code "validation_error"}), or {@code null} when the body had none.
   */
  public String getCode() {
    return code;
  }

  /** The parsed response body when the API returned JSON, otherwise {@code null}. */
  public JsonNode getBody() {
    return body;
  }

  /**
   * Map an HTTP status to the most specific {@link ApiException} subclass.
   *
   * @param retryAfter seconds from the {@code Retry-After} header, or {@code null}
   */
  public static ApiException fromStatus(
      int status, String code, String message, JsonNode body, Long retryAfter) {
    if (status == 400 || status == 422) {
      return new ValidationException(status, code, message, body);
    }
    if (status == 401) {
      return new AuthenticationException(status, code, message, body);
    }
    if (status == 403) {
      return new PermissionDeniedException(status, code, message, body);
    }
    if (status == 404) {
      return new NotFoundException(status, code, message, body);
    }
    if (status == 429) {
      return new RateLimitException(status, code, message, body, retryAfter);
    }
    if (status >= 500) {
      return new ServerException(status, code, message, body);
    }
    return new ApiException(status, code, message, body);
  }
}
