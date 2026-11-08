package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * An id stays inside one encoded path segment, and an id of "", "." or ".." is refused before
 * anything is sent: URL normalisation would drop that segment or climb a level and reach another
 * endpoint of the same API key.
 */
class PathSegmentsTest {

  @Test
  void hostileIdStaysInsideOneSegment() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.add(
                  exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath());
              TestServer.respond(exchange, 200, "{\"data\":{}}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      client.posts().publish("../webhooks/wh_fake/rotate-secret#");

      assertEquals(
          "POST /posts/..%2Fwebhooks%2Fwh_fake%2Frotate-secret%23/publish", requests.get(0));
    }
  }

  @Test
  void dotAndEmptyIdsAreRefusedBeforeSending() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.add(exchange.getRequestURI().getRawPath());
              TestServer.respond(exchange, 200, "{\"data\":{}}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(0)
              .build();

      for (String bad : new String[] {"", ".", ".."}) {
        Executable[] calls = {
          () -> client.posts().delete(bad),
          () -> client.webhooks().rotateSecret(bad),
          () -> client.inbox().markRead(bad),
          () -> client.ads().drafts().get(bad),
        };
        for (Executable call : calls) {
          IllegalArgumentException error = assertThrows(IllegalArgumentException.class, call);
          assertTrue(error.getMessage().startsWith("Invalid request path"), error.getMessage());
        }
      }
      assertTrue(requests.isEmpty(), "a refused id sends nothing: " + requests);
    }
  }
}
