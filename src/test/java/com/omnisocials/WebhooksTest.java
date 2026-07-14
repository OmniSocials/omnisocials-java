package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.errors.WebhookVerificationException;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Signs payloads exactly like the backend's webhookDispatcher.js:
 * HMAC-SHA256 hex over {@code ts + "." + body}, header {@code t=<ts>,v1=<hex>}.
 */
class WebhooksTest {

  private static final String SECRET = "whsec_test_secret_42";
  private static final String BODY =
      "{\"id\":\"evt_123\",\"type\":\"post.published\","
          + "\"data\":{\"post_id\":\"p_1\",\"targets\":[\"instagram\"]}}";

  /** Mirror of webhookDispatcher.js signPayload(). */
  private static String sign(String secret, long timestamp, String rawBody) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] digest = mac.doFinal((timestamp + "." + rawBody).getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : digest) {
      hex.append(String.format("%02x", b));
    }
    return "t=" + timestamp + ",v1=" + hex;
  }

  @Test
  void validSignatureRoundTrips() throws Exception {
    long ts = System.currentTimeMillis() / 1000L;
    String header = sign(SECRET, ts, BODY);

    JsonNode event = Webhooks.verifySignature(BODY, header, SECRET, 300);

    assertEquals("post.published", event.get("type").asText());
    assertEquals("p_1", event.get("data").get("post_id").asText());
  }

  @Test
  void tamperedPayloadFails() throws Exception {
    long ts = System.currentTimeMillis() / 1000L;
    String header = sign(SECRET, ts, BODY);
    String tampered = BODY.replace("p_1", "p_EVIL");

    WebhookVerificationException e =
        assertThrows(
            WebhookVerificationException.class,
            () -> Webhooks.verifySignature(tampered, header, SECRET, 300));
    assertTrue(e.getMessage().contains("verification failed"));
  }

  @Test
  void wrongSecretFails() throws Exception {
    long ts = System.currentTimeMillis() / 1000L;
    String header = sign("whsec_other", ts, BODY);

    assertThrows(
        WebhookVerificationException.class,
        () -> Webhooks.verifySignature(BODY, header, SECRET, 300));
  }

  @Test
  void staleTimestampFails() throws Exception {
    long stale = System.currentTimeMillis() / 1000L - 3600; // 1 hour old
    String header = sign(SECRET, stale, BODY);

    WebhookVerificationException e =
        assertThrows(
            WebhookVerificationException.class,
            () -> Webhooks.verifySignature(BODY, header, SECRET, 300));
    assertTrue(e.getMessage().contains("tolerance"));
  }

  @Test
  void toleranceZeroDisablesStalenessCheck() throws Exception {
    long stale = System.currentTimeMillis() / 1000L - 3600;
    String header = sign(SECRET, stale, BODY);

    JsonNode event = Webhooks.verifySignature(BODY, header, SECRET, 0);
    assertEquals("evt_123", event.get("id").asText());
  }

  @Test
  void defaultToleranceIsFiveMinutes() throws Exception {
    long ts = System.currentTimeMillis() / 1000L - 100; // within 300s
    String header = sign(SECRET, ts, BODY);

    JsonNode event = Webhooks.verifySignature(BODY, header, SECRET);
    assertEquals("evt_123", event.get("id").asText());
  }

  @Test
  void malformedHeaderFails() {
    assertThrows(
        WebhookVerificationException.class,
        () -> Webhooks.verifySignature(BODY, "not-a-signature", SECRET, 300));
    assertThrows(
        WebhookVerificationException.class,
        () -> Webhooks.verifySignature(BODY, "t=notanumber,v1=abc", SECRET, 300));
    assertThrows(
        WebhookVerificationException.class,
        () -> Webhooks.verifySignature(BODY, "t=123456", SECRET, 300));
    assertThrows(
        WebhookVerificationException.class, () -> Webhooks.verifySignature(BODY, "", SECRET, 300));
    assertThrows(
        WebhookVerificationException.class,
        () -> Webhooks.verifySignature(BODY, "t=1,v1=abc", "", 300));
  }

  @Test
  void extraAndUnknownPairsAreTolerated() throws Exception {
    long ts = System.currentTimeMillis() / 1000L;
    String valid = sign(SECRET, ts, BODY); // t=..,v1=..
    String hex = valid.substring(valid.indexOf("v1=") + 3);
    String header = "t=" + ts + ",v0=deadbeef,v1=" + hex + ",unknown=1";

    JsonNode event = Webhooks.verifySignature(BODY, header, SECRET, 300);
    assertEquals("evt_123", event.get("id").asText());
  }

  @Test
  void nonJsonPayloadFails() throws Exception {
    String payload = "this is not json {{{";
    long ts = System.currentTimeMillis() / 1000L;
    String header = sign(SECRET, ts, payload);

    WebhookVerificationException e =
        assertThrows(
            WebhookVerificationException.class,
            () -> Webhooks.verifySignature(payload, header, SECRET, 300));
    assertTrue(e.getMessage().contains("not valid JSON"));
  }
}
