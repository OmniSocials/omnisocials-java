package com.omnisocials.errors;

/**
 * Base class for every exception thrown by the OmniSocials SDK.
 *
 * <pre>
 * OmniSocialsException                (base for everything the SDK throws)
 *   ApiException                      (non-2xx HTTP response; has status/code/body)
 *     AuthenticationException         (401)
 *     PermissionDeniedException       (403)
 *     NotFoundException               (404)
 *     ValidationException             (400 / 422)
 *     RateLimitException              (429; exposes getRetryAfter() seconds)
 *     ServerException                 (&gt;= 500)
 *   ApiConnectionException            (network failure / timeout)
 *   WebhookVerificationException      (invalid webhook signature)
 * </pre>
 *
 * All exceptions are unchecked so resource calls stay ergonomic; catch
 * {@link ApiException} (or a subclass) around calls you want to handle.
 */
public class OmniSocialsException extends RuntimeException {

  public OmniSocialsException(String message) {
    super(message);
  }

  public OmniSocialsException(String message, Throwable cause) {
    super(message, cause);
  }
}
