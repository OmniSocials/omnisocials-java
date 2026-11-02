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

/** Request paths, bodies and response parsing of the post tag endpoints. */
class TagsResourceTest {

  private static final String TAG =
      "{\"id\":12,\"name\":\"Question\",\"color\":\"blue\","
          + "\"created_at\":\"2026-11-02T10:00:00.000Z\"}";

  @Test
  void crudAndApplySendTheRightRequests() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              String body =
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              String path = exchange.getRequestURI().getRawPath();
              requests.add(exchange.getRequestMethod() + " " + path + " " + body);
              String response;
              int status = 200;
              if (path.equals("/tags/apply")) {
                response =
                    "{\"data\":{\"updated\":1,\"posts\":[{\"id\":\"1024\",\"tags\":["
                        + TAG
                        + "]}],\"not_found\":[\"999\"]}}";
              } else if (exchange.getRequestMethod().equals("DELETE")) {
                response = "{\"data\":{\"tag\":" + TAG + ",\"posts_untagged\":3}}";
              } else if (exchange.getRequestMethod().equals("GET")) {
                response = "{\"data\":[" + TAG + "]}";
              } else {
                if (exchange.getRequestMethod().equals("POST")) status = 201;
                response = "{\"data\":" + TAG + "}";
              }
              TestServer.respond(exchange, status, response);
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode list = client.tags().list();
      JsonNode created = client.tags().create(Params.of("name", "Question", "color", "blue"));
      client.tags().update(12, Params.of("name", "Questions"));
      JsonNode deleted = client.tags().delete("12");
      JsonNode applied =
          client
              .tags()
              .apply(
                  Params.builder()
                      .put("post_ids", List.of("1024", "999"))
                      .put("add", List.of("Question", 7))
                      .build());

      assertEquals("Question", list.get("data").get(0).get("name").asText());
      assertFalse(list.get("data").get(0).has("group"));
      assertEquals(12, created.get("data").get("id").asInt());
      assertEquals(3, deleted.get("data").get("posts_untagged").asInt());
      assertEquals(1, applied.get("data").get("updated").asInt());
      assertEquals("999", applied.get("data").get("not_found").get(0).asText());
      assertEquals(
          "blue", applied.get("data").get("posts").get(0).get("tags").get(0).get("color").asText());

      assertEquals("GET /tags ", requests.get(0));
      assertEquals("POST /tags {\"name\":\"Question\",\"color\":\"blue\"}", requests.get(1));
      assertEquals("PATCH /tags/12 {\"name\":\"Questions\"}", requests.get(2));
      assertEquals("DELETE /tags/12 ", requests.get(3));
      assertEquals(
          "POST /tags/apply {\"post_ids\":[\"1024\",\"999\"],\"add\":[\"Question\",7]}",
          requests.get(4));
    }
  }

  @Test
  void analyticsTagsSendsTheQueryAndParsesTheComparison() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.add(
                  exchange.getRequestURI().getPath()
                      + "?"
                      + exchange.getRequestURI().getRawQuery());
              TestServer.respond(
                  exchange,
                  200,
                  "{\"data\":{\"period\":{\"start_date\":\"2026-08-01T00:00:00.000Z\","
                      + "\"end_date\":\"2026-10-31T23:59:59.999Z\"},\"platform\":\"tiktok\","
                      + "\"min_posts\":3,\"tags\":[{\"tag\":"
                      + TAG
                      + ",\"posts\":9,\"measured\":8,\"median_impressions\":1520,"
                      + "\"engagement_rate\":5.4,\"vs_normal\":0.38,\"best_channel\":\"tiktok\","
                      + "\"enough_posts\":true}],\"untagged\":{\"posts\":4,\"measured\":4,"
                      + "\"median_impressions\":900,\"engagement_rate\":3.1,\"vs_normal\":-0.1,"
                      + "\"best_channel\":null,\"enough_posts\":false},"
                      + "\"normals\":{\"tiktok\":1100}}}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result =
          client
              .analytics()
              .tags(
                  Params.builder()
                      .put("start_date", "2026-08-01")
                      .put("end_date", "2026-10-31")
                      .put("platform", "tiktok")
                      .put("min_posts", 3)
                      .build());
      client.posts().list(Params.of("tag", "Question,7"));
      client.analytics().overview(Params.of("period", "30d", "tag", "Question"));

      assertEquals(
          "/analytics/tags?start_date=2026-08-01&end_date=2026-10-31&platform=tiktok&min_posts=3",
          requests.get(0));
      assertEquals("/posts?tag=Question%2C7", requests.get(1));
      assertEquals("/analytics/overview?period=30d&tag=Question", requests.get(2));

      JsonNode data = result.get("data");
      assertFalse(data.has("groups"));
      JsonNode first = data.get("tags").get(0);
      assertEquals("Question", first.get("tag").get("name").asText());
      assertEquals(0.38, first.get("vs_normal").asDouble());
      assertTrue(first.get("enough_posts").asBoolean());
      assertTrue(data.get("untagged").get("best_channel").isNull());
      assertEquals(1100, data.get("normals").get("tiktok").asInt());
    }
  }

  @Test
  void tagExistsCarriesTheExistingTagInTheBody() throws IOException {
    try (TestServer server =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    409,
                    "{\"error\":{\"code\":\"tag_exists\",\"message\":\"A tag with this name"
                        + " exists.\",\"tag\":"
                        + TAG
                        + "}}"))) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(0)
              .build();

      ApiException error =
          assertThrows(
              ApiException.class, () -> client.tags().create(Params.of("name", "Question")));
      assertEquals(409, error.getStatus());
      assertEquals("tag_exists", error.getCode());
      assertEquals(12, error.getBody().get("error").get("tag").get("id").asInt());
    }
  }
}
