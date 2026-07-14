package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnisocials.errors.ApiConnectionException;
import com.omnisocials.errors.AuthenticationException;
import com.omnisocials.errors.NotFoundException;
import com.omnisocials.errors.PermissionDeniedException;
import com.omnisocials.errors.RateLimitException;
import com.omnisocials.errors.ServerException;
import com.omnisocials.errors.ValidationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

/** Maps API status codes to the exception hierarchy using a local HTTP stub. */
class ErrorMappingTest {

  private static OmniSocials client(TestServer server, int maxRetries) {
    return OmniSocials.builder()
        .apiKey("omsk_test_key")
        .baseUrl(server.baseUrl())
        .maxRetries(maxRetries)
        .build();
  }

  @Test
  void notFoundMapsToNotFoundException() throws IOException {
    try (TestServer server =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    404,
                    "{\"error\":{\"code\":\"not_found\",\"message\":\"Post not found\"}}"))) {
      NotFoundException e =
          assertThrows(
              NotFoundException.class, () -> client(server, 2).posts().get("missing-post"));
      assertEquals(404, e.getStatus());
      assertEquals("not_found", e.getCode());
      assertEquals("Post not found", e.getMessage());
      assertNotNull(e.getBody());
      assertEquals("not_found", e.getBody().get("error").get("code").asText());
    }
  }

  @Test
  void validationAuthAndPermissionMapping() throws IOException {
    try (TestServer server400 =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    400,
                    "{\"error\":{\"code\":\"validation_error\",\"message\":\"content is required\"}}"))) {
      ValidationException e =
          assertThrows(
              ValidationException.class,
              () -> client(server400, 0).posts().create(Params.of("channels", "x")));
      assertEquals("validation_error", e.getCode());
    }

    try (TestServer server401 =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    401,
                    "{\"error\":{\"code\":\"invalid_api_key\",\"message\":\"Invalid key\"}}"))) {
      assertThrows(AuthenticationException.class, () -> client(server401, 0).accounts().list());
    }

    try (TestServer server403 =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    403,
                    "{\"error\":{\"code\":\"insufficient_scope\",\"message\":\"Missing scope\"}}"))) {
      assertThrows(PermissionDeniedException.class, () -> client(server403, 0).folders().list());
    }
  }

  @Test
  void rateLimitExposesRetryAfter() throws IOException {
    try (TestServer server =
        new TestServer(
            exchange -> {
              exchange.getResponseHeaders().set("Retry-After", "7");
              TestServer.respond(
                  exchange,
                  429,
                  "{\"error\":{\"code\":\"rate_limit_exceeded\",\"message\":\"Slow down\"}}");
            })) {
      RateLimitException e =
          assertThrows(RateLimitException.class, () -> client(server, 0).posts().list());
      assertEquals(429, e.getStatus());
      assertEquals(Long.valueOf(7L), e.getRetryAfter());
    }
  }

  @Test
  void serverErrorMapsToServerException() throws IOException {
    try (TestServer server =
        new TestServer(
            exchange ->
                TestServer.respond(
                    exchange,
                    500,
                    "{\"error\":{\"code\":\"internal_error\",\"message\":\"Boom\"}}"))) {
      ServerException e =
          assertThrows(ServerException.class, () -> client(server, 0).posts().list());
      assertEquals(500, e.getStatus());
    }
  }

  @Test
  void nonJsonErrorBodyStillMaps() throws IOException {
    try (TestServer server =
        new TestServer(exchange -> TestServer.respond(exchange, 404, "<html>nope</html>"))) {
      NotFoundException e =
          assertThrows(NotFoundException.class, () -> client(server, 0).media().get("x"));
      assertEquals(404, e.getStatus());
      assertNull(e.getCode());
      assertEquals("Request failed with status 404.", e.getMessage());
    }
  }

  @Test
  void deleteReturnsNullOn204() throws IOException {
    try (TestServer server =
        new TestServer(exchange -> TestServer.respond(exchange, 204, null))) {
      assertNull(client(server, 0).posts().delete("p_1"));
    }
  }

  @Test
  void connectionErrorThrowsApiConnectionException() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket()) {
      socket.bind(new InetSocketAddress("127.0.0.1", 0));
      closedPort = socket.getLocalPort();
    } // closed again: nothing listens here now

    OmniSocials client =
        OmniSocials.builder()
            .apiKey("omsk_test_key")
            .baseUrl("http://127.0.0.1:" + closedPort)
            .maxRetries(0)
            .build();
    assertThrows(ApiConnectionException.class, client::health);
  }
}
