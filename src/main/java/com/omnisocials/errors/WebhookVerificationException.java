package com.omnisocials.errors;

/** A webhook delivery failed signature verification (bad signature, stale timestamp, bad payload). */
public class WebhookVerificationException extends OmniSocialsException {

  public WebhookVerificationException(String message) {
    super(message);
  }
}
