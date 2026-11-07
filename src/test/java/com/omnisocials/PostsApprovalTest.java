package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnisocials.errors.ApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Request bodies of approve and reject, and the 409 step_changed answer. */
class PostsApprovalTest {

  @Test
  void approveAndRejectSendStepOrderOnlyWhenGiven() throws IOException {
    List<String> requests = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server =
        new TestServer(
            exchange -> {
              String body =
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              requests.add(
                  exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath() + " " + body);
              if (body.contains("\"step_order\":1")) {
                TestServer.respond(
                    exchange,
                    409,
                    "{\"error\":{\"code\":\"step_changed\","
                        + "\"message\":\"The review is on step 2 now.\",\"current_step_order\":2}}");
                return;
              }
              TestServer.respond(
                  exchange,
                  200,
                  "{\"data\":{\"id\":\"1024\",\"status\":\"in_approval\",\"approval_status\":\"pending\"}}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_test_key")
              .baseUrl(server.baseUrl())
              .maxRetries(0)
              .build();

      client.posts().approve("1024");
      client.posts().approve("1024", null);
      client.posts().approve("1024", 2);
      client.posts().reject("1024");
      client.posts().reject("1024", "Wrong CTA link");
      client.posts().reject("1024", "Wrong CTA link", 2);
      client.posts().reject("1024", null, 3);

      assertEquals("POST /posts/1024/approve ", requests.get(0));
      assertEquals("POST /posts/1024/approve ", requests.get(1));
      assertEquals("POST /posts/1024/approve {\"step_order\":2}", requests.get(2));
      assertEquals("POST /posts/1024/reject ", requests.get(3));
      assertEquals("POST /posts/1024/reject {\"comment\":\"Wrong CTA link\"}", requests.get(4));
      assertEquals(
          "POST /posts/1024/reject {\"comment\":\"Wrong CTA link\",\"step_order\":2}",
          requests.get(5));
      assertEquals("POST /posts/1024/reject {\"step_order\":3}", requests.get(6));

      ApiException err = assertThrows(ApiException.class, () -> client.posts().approve("1024", 1));
      assertEquals(409, err.getStatus());
      assertEquals("step_changed", err.getCode());
      assertEquals(2, err.getBody().path("error").path("current_step_order").asInt());
    }
  }
}
