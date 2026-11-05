package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.errors.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Request paths, queries, bodies and responses of the limits and feature request endpoints. */
class FeatureRequestsResourceTest {

  private static final String LIMIT =
      "{\"id\":\"tiktok.delete\",\"kind\":\"platform\","
          + "\"kind_label\":\"The platform does not allow it\",\"area\":\"posts\","
          + "\"platforms\":[\"tiktok\"],\"title\":\"Delete a TikTok video\","
          + "\"says\":\"TikTok's API cannot delete a video.\","
          + "\"instead\":\"Delete the video in the TikTok app.\",\"requestable\":false}";

  private static final String REQUEST =
      "{\"id\":412,\"title\":\"Post to a topic of a Telegram group\","
          + "\"summary\":\"Post our daily update to the Announcements topic.\",\"status\":\"open\","
          + "\"team_answer\":null,\"team_replies\":[],\"votes_count\":1,"
          + "\"limit_id\":\"telegram.forum_topics\",\"platform\":\"telegram\",\"area\":\"posts\","
          + "\"source\":\"api\",\"created_at\":\"2026-11-04T10:00:00.000Z\","
          + "\"updated_at\":\"2026-11-04T10:00:00.000Z\",\"completed_at\":null}";

  @Test
  void limitsAndRequestsSendTheRightRequests() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              String body =
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              String path = exchange.getRequestURI().getRawPath();
              String query = exchange.getRequestURI().getRawQuery();
              String method = exchange.getRequestMethod();
              requests.add(method + " " + path + (query == null ? "" : "?" + query) + " " + body);
              if (path.equals("/limits")) {
                TestServer.respond(
                    exchange,
                    200,
                    "{\"data\":[" + LIMIT + "],\"kinds\":{\"platform\":\"The platform does not allow it\"}}");
              } else if (method.equals("POST") && body.contains("\"Again")) {
                TestServer.respond(
                    exchange, 200, "{\"outcome\":\"already_requested\",\"data\":" + REQUEST + "}");
              } else if (method.equals("POST")) {
                TestServer.respond(exchange, 201, "{\"outcome\":\"voted\",\"data\":" + REQUEST + "}");
              } else if (path.equals("/feature-requests")) {
                TestServer.respond(exchange, 200, "{\"data\":[" + REQUEST + "]}");
              } else {
                TestServer.respond(exchange, 200, "{\"data\":" + REQUEST + "}");
              }
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode limits = client.limits().list(Params.of("platform", "tiktok", "area", "posts"));
      client.limits().list();
      JsonNode voted =
          client
              .featureRequests()
              .create(
                  Params.builder()
                      .put("summary", "Post our daily update to the Announcements topic.")
                      .put("platform", "telegram")
                      .put("limit_id", "telegram.forum_topics")
                      .build());
      JsonNode again =
          client.featureRequests().create(Params.of("summary", "Again: a Telegram topic, please."));
      JsonNode mine = client.featureRequests().list();
      JsonNode one = client.featureRequests().get(412);

      assertFalse(limits.get("data").get(0).get("requestable").asBoolean());
      assertEquals("The platform does not allow it", limits.get("kinds").get("platform").asText());
      assertEquals("voted", voted.get("outcome").asText());
      assertEquals(412, voted.get("data").get("id").asInt());
      assertEquals("already_requested", again.get("outcome").asText());
      assertEquals("open", mine.get("data").get(0).get("status").asText());
      assertTrue(one.get("data").get("team_answer").isNull());
      assertEquals("telegram.forum_topics", one.get("data").get("limit_id").asText());

      assertEquals("GET /limits?platform=tiktok&area=posts ", requests.get(0));
      assertEquals("GET /limits ", requests.get(1));
      assertEquals(
          "POST /feature-requests {\"summary\":\"Post our daily update to the Announcements"
              + " topic.\",\"platform\":\"telegram\",\"limit_id\":\"telegram.forum_topics\"}",
          requests.get(2));
      assertEquals("GET /feature-requests ", requests.get(4));
      assertEquals("GET /feature-requests/412 ", requests.get(5));
    }
  }

  @Test
  void notRequestableCarriesTheLimitInTheBody() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.add(exchange.getRequestMethod());
              TestServer.respond(
                  exchange,
                  409,
                  "{\"error\":{\"code\":\"not_requestable\",\"message\":\"TikTok's API cannot delete"
                      + " a video. A request cannot change this, so nothing was sent.\",\"limit\":"
                      + LIMIT
                      + "}}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      ApiException error =
          assertThrows(
              ApiException.class,
              () ->
                  client
                      .featureRequests()
                      .create(
                          Params.of(
                              "summary", "Delete my TikTok videos.", "limit_id", "tiktok.delete")));
      assertEquals(409, error.getStatus());
      assertEquals("not_requestable", error.getCode());
      assertEquals(
          "Delete the video in the TikTok app.",
          error.getBody().get("error").get("limit").get("instead").asText());
      assertEquals(1, requests.size(), "a 409 is not retried");
    }
  }
}
