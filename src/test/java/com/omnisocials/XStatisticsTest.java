package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.errors.ApiConnectionException;
import com.omnisocials.errors.ApiException;
import com.omnisocials.errors.ServerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** X statistics on credits: accounts().update, analytics().refreshPost, credits().get. */
class XStatisticsTest {

  /** Records "METHOD /path body" per request and answers with the given status and body. */
  private static TestServer recording(List<String> seen, int status, String body) throws IOException {
    return new TestServer(
        exchange -> {
          String requestBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          seen.add(
              exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath() + " " + requestBody);
          exchange.getResponseHeaders().set("Retry-After", "0");
          TestServer.respond(exchange, status, body);
        });
  }

  private static OmniSocials client(TestServer server) {
    return OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).maxRetries(2).build();
  }

  @Test
  void accountsUpdateSendsOnlyTheGivenKeys() throws IOException {
    List<String> seen = Collections.synchronizedList(new ArrayList<>());
    String account =
        "{\"data\":{\"id\":\"1234_x\",\"platform\":\"x\",\"x_statistics\":{\"posts\":\"light\","
            + "\"followers\":\"daily\",\"post_schedule\":null,\"source\":\"account\",\"billing\":\"credits\","
            + "\"state\":\"active\",\"paused_reason\":null}}}";
    try (TestServer server = recording(seen, 200, account)) {
      OmniSocials client = client(server);

      JsonNode updated =
          client
              .accounts()
              .update(
                  "1234_x",
                  Params.of("x_statistics", Params.of("posts", "light", "followers", "daily")));
      client.accounts().updateXStatistics("1234_x", Params.of("followers", "off"));
      client
          .accounts()
          .updateXStatistics(
              "1234_x",
              Params.builder()
                  .put("posts", "custom")
                  .put(
                      "post_schedule",
                      Params.of("at", List.of("1h", "1d"), "then_every", "7d", "until", "90d"))
                  .build());
      client.accounts().updateXStatistics("1234_x", null);

      assertEquals("light", updated.get("data").get("x_statistics").get("posts").asText());
      assertEquals(
          "PATCH /accounts/1234_x {\"x_statistics\":{\"posts\":\"light\",\"followers\":\"daily\"}}",
          seen.get(0));
      assertEquals("PATCH /accounts/1234_x {\"x_statistics\":{\"followers\":\"off\"}}", seen.get(1));
      assertEquals(
          "PATCH /accounts/1234_x {\"x_statistics\":{\"posts\":\"custom\",\"post_schedule\":"
              + "{\"at\":[\"1h\",\"1d\"],\"then_every\":\"7d\",\"until\":\"90d\"}}}",
          seen.get(2));
      assertEquals("PATCH /accounts/1234_x {\"x_statistics\":null}", seen.get(3), "null is sent as null");
    }
  }

  @Test
  void refreshPostAndCreditsSendTheRightRequests() throws IOException {
    List<String> seen = Collections.synchronizedList(new ArrayList<>());
    String body =
        "{\"data\":{\"post_id\":9123,\"tweets\":[{\"id\":\"1859\",\"metrics\":{\"views\":3412},"
            + "\"read_at\":\"2027-01-08T09:12:04Z\"}],\"credits_used\":0.5,\"read_at\":\"2027-01-08T09:12:04Z\","
            + "\"balance\":412}}";
    try (TestServer server = recording(seen, 200, body)) {
      OmniSocials client = client(server);

      JsonNode fresh = client.analytics().refreshPost(9123);
      client.analytics().refreshPost("9123");
      JsonNode credits = client.credits().get();

      assertEquals(0.5, fresh.get("data").get("credits_used").asDouble());
      assertEquals("POST /analytics/posts/9123/refresh ", seen.get(0));
      assertEquals("POST /analytics/posts/9123/refresh ", seen.get(1));
      assertEquals("GET /credits ", seen.get(2));
      assertEquals(412, credits.get("data").get("balance").asInt());
    }
  }

  @Test
  void refreshPostIsSentOnceAfterA502() throws IOException {
    List<String> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        recording(seen, 502, "{\"error\":{\"code\":\"refresh_failed\",\"message\":\"X did not answer\"}}")) {
      OmniSocials client = client(server);
      ServerException e = assertThrows(ServerException.class, () -> client.analytics().refreshPost(9123));
      assertEquals("refresh_failed", e.getCode());
      assertEquals(1, seen.size(), "a refresh must not be retried after a 502");

      seen.clear();
      assertThrows(ServerException.class, () -> client.credits().get());
      assertEquals(3, seen.size(), "a read is retried after a 502 (initial attempt + 2 retries)");
    }
  }

  @Test
  void refreshPostWhoseConnectionDropsIsSentOnce() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.incrementAndGet();
              exchange.getRequestBody().readAllBytes();
              exchange.close();
            })) {
      OmniSocials client = client(server);
      assertThrows(ApiConnectionException.class, () -> client.analytics().refreshPost(9123));
      assertEquals(1, requests.get(), "a refresh must not be retried after a connection error");
    }
  }

  @Test
  void refreshPostIsRetriedAfterA429() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              exchange.getResponseHeaders().set("Retry-After", "0");
              if (requests.incrementAndGet() == 1) {
                TestServer.respond(exchange, 429, "{\"error\":{\"code\":\"rate_limit_exceeded\"}}");
              } else {
                TestServer.respond(exchange, 200, "{\"data\":{\"post_id\":9123,\"tweets\":[],\"credits_used\":0}}");
              }
            })) {
      OmniSocials client = client(server);
      JsonNode fresh = client.analytics().refreshPost(9123);
      assertEquals(2, requests.get(), "a 429 is retried");
      assertEquals(0, fresh.get("data").get("credits_used").asInt());
    }
  }

  @Test
  void insufficientCreditsIsA402ApiException() throws IOException {
    List<String> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        recording(seen, 402, "{\"error\":{\"code\":\"insufficient_credits\",\"message\":\"Not enough credits\"}}")) {
      OmniSocials client = client(server);
      ApiException e = assertThrows(ApiException.class, () -> client.analytics().refreshPost(9123));
      assertEquals(402, e.getStatus());
      assertEquals("insufficient_credits", e.getCode());
      assertEquals(1, seen.size());
    }
  }
}
