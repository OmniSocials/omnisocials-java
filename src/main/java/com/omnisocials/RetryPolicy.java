package com.omnisocials;

/**
 * Which failures a request is sent again on. Pass it to the
 * {@link OmniSocials#post(String, java.util.Map, RetryPolicy)} family of helpers.
 */
public enum RetryPolicy {

  /** Retry a 429, a 5xx and a connection error or timeout (the default). */
  DEFAULT,

  /**
   * Retry only a 429. A call that can start or raise ad spend uses it: after a
   * 5xx or a timeout the first attempt may have gone through, so it is never
   * sent twice.
   */
  RATE_LIMIT_ONLY
}
