package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.errors.RateLimitException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Automatic retry behavior on 429 / 5xx. */
class RetryTest {

  @Test
  void retriesOn429ThenSucceeds() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              if (requests.incrementAndGet() == 1) {
                exchange.getResponseHeaders().set("Retry-After", "0");
                TestServer.respond(
                    exchange,
                    429,
                    "{\"error\":{\"code\":\"rate_limit_exceeded\",\"message\":\"Slow down\"}}");
              } else {
                TestServer.respond(exchange, 200, "{\"data\":[{\"id\":\"p_1\"}]}");
              }
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(2)
              .build();

      JsonNode result = client.posts().list();

      assertEquals(2, requests.get(), "should have retried exactly once");
      assertEquals("p_1", result.get("data").get(0).get("id").asText());
    }
  }

  @Test
  void retriesOn500ThenSucceeds() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              if (requests.incrementAndGet() == 1) {
                TestServer.respond(
                    exchange, 500, "{\"error\":{\"code\":\"internal_error\",\"message\":\"Boom\"}}");
              } else {
                TestServer.respond(exchange, 200, "{\"data\":{\"status\":\"ok\"}}");
              }
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(1)
              .build();

      JsonNode result = client.health();

      assertEquals(2, requests.get());
      assertEquals("ok", result.get("data").get("status").asText());
    }
  }

  @Test
  void exhaustedRetriesThrowRateLimitException() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.incrementAndGet();
              exchange.getResponseHeaders().set("Retry-After", "0");
              TestServer.respond(
                  exchange,
                  429,
                  "{\"error\":{\"code\":\"rate_limit_exceeded\",\"message\":\"Slow down\"}}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(2)
              .build();

      assertThrows(RateLimitException.class, () -> client.posts().list());
      assertEquals(3, requests.get(), "initial attempt + 2 retries");
    }
  }

  @Test
  void doesNotRetryOn4xxOtherThan429() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.incrementAndGet();
              TestServer.respond(
                  exchange,
                  400,
                  "{\"error\":{\"code\":\"validation_error\",\"message\":\"Bad\"}}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(2)
              .build();

      assertThrows(Exception.class, () -> client.posts().create(Params.of("content", "x")));
      assertTrue(requests.get() == 1, "4xx (non-429) must not be retried");
    }
  }
}
