package com.omnisocials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisocials.errors.WebhookVerificationException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Webhook signature verification for OmniSocials event deliveries
 * (Stripe-style scheme).
 *
 * <p>Every delivery carries an {@code X-OmniSocials-Signature} header of the
 * form {@code t=<unix>,v1=<hex>}, where the hex value is an HMAC-SHA256 of
 * {@code "{timestamp}.{rawBody}"} computed with your webhook signing secret.
 * Always verify against the RAW request body, exactly as received. Do not
 * parse and re-serialize it first.
 */
public final class Webhooks {

  /** Default max allowed age of the signed timestamp: 300 seconds (5 minutes). */
  public static final long DEFAULT_TOLERANCE_SECONDS = 300;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Webhooks() {}

  /** Verify with the default tolerance of {@value #DEFAULT_TOLERANCE_SECONDS} seconds. */
  public static JsonNode verifySignature(String payload, String signature, String secret) {
    return verifySignature(payload, signature, secret, DEFAULT_TOLERANCE_SECONDS);
  }

  /**
   * Verify an OmniSocials webhook delivery.
   *
   * <p>Uses a constant-time comparison ({@link MessageDigest#isEqual}) and
   * rejects timestamps older than {@code toleranceSeconds} (replay
   * protection). Pass {@code 0} or a negative tolerance to skip the timestamp
   * age check (not recommended).
   *
   * @param payload the RAW request body, exactly as received
   * @param signature the {@code X-OmniSocials-Signature} header value: {@code t=<unix>,v1=<hex>}
   * @param secret the webhook signing secret (shown once on create / rotate-secret)
   * @param toleranceSeconds max allowed age of the timestamp, in seconds
   * @return the parsed event object on success
   * @throws WebhookVerificationException on any failure
   */
  public static JsonNode verifySignature(
      String payload, String signature, String secret, long toleranceSeconds) {
    if (secret == null || secret.isEmpty()) {
      throw new WebhookVerificationException("No webhook secret provided.");
    }
    if (signature == null || signature.isEmpty()) {
      throw new WebhookVerificationException(
          "No signature header provided. Expected the X-OmniSocials-Signature header value.");
    }
    if (payload == null) {
      throw new WebhookVerificationException("No payload provided.");
    }

    // Parse `t=<unix>,v1=<hex>` (tolerate extra/unknown pairs and multiple v1).
    String timestampRaw = null;
    List<String> candidateSignatures = new ArrayList<>();
    for (String part : signature.split(",")) {
      int eq = part.indexOf('=');
      if (eq == -1) {
        continue;
      }
      String key = part.substring(0, eq).trim();
      String value = part.substring(eq + 1).trim();
      if (key.equals("t")) {
        timestampRaw = value;
      } else if (key.equals("v1")) {
        candidateSignatures.add(value);
      }
    }

    if (timestampRaw == null || !timestampRaw.matches("-?\\d+")) {
      throw new WebhookVerificationException(
          "Unable to extract timestamp from signature header. Expected format: t=<unix>,v1=<hex>.");
    }
    if (candidateSignatures.isEmpty()) {
      throw new WebhookVerificationException(
          "Unable to extract v1 signature from signature header. Expected format: t=<unix>,v1=<hex>.");
    }

    String expected = hmacSha256Hex(secret, timestampRaw + "." + payload);
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);

    boolean matches = false;
    for (String candidate : candidateSignatures) {
      byte[] candidateBytes = candidate.getBytes(StandardCharsets.UTF_8);
      if (candidateBytes.length == expectedBytes.length
          && MessageDigest.isEqual(candidateBytes, expectedBytes)) {
        matches = true;
      }
    }
    if (!matches) {
      throw new WebhookVerificationException(
          "Webhook signature verification failed: no v1 signature matches the expected signature.");
    }

    long timestamp = Long.parseLong(timestampRaw);
    long nowSeconds = System.currentTimeMillis() / 1000L;
    if (toleranceSeconds > 0 && nowSeconds - timestamp > toleranceSeconds) {
      throw new WebhookVerificationException(
          "Webhook timestamp is outside the allowed tolerance of " + toleranceSeconds + "s "
              + "(event is " + (nowSeconds - timestamp) + "s old). Possible replay.");
    }

    try {
      return MAPPER.readTree(payload);
    } catch (Exception e) {
      throw new WebhookVerificationException(
          "Webhook payload is not valid JSON (did you pass the raw request body?).");
    }
  }

  private static String hmacSha256Hex(String secret, String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (GeneralSecurityException e) {
      // HmacSHA256 is guaranteed on every Java platform; this cannot happen.
      throw new IllegalStateException("HmacSHA256 unavailable", e);
    }
  }
}
