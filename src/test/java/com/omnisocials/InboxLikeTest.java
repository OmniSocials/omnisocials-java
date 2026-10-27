package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Request path, body and response parsing of {@code inbox().like()}. */
class InboxLikeTest {

  @Test
  void likePostsToTheLikePathAndParsesLiked() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              String body =
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              requests.add(
                  exchange.getRequestMethod()
                      + " "
                      + exchange.getRequestURI().getRawPath()
                      + " "
                      + body);
              TestServer.respond(
                  exchange,
                  200,
                  "{\"data\":{\"id\":\"123\",\"platform\":\"facebook\",\"type\":\"comment\","
                      + "\"hidden\":false,\"liked\":true}}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode liked = client.inbox().like("123");
      client.inbox().like("123", false);

      assertTrue(liked.get("data").get("liked").asBoolean());
      assertEquals("POST /inbox/messages/123/like ", requests.get(0));
      assertEquals("POST /inbox/messages/123/like {\"like\":false}", requests.get(1));
    }
  }
}
