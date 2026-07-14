package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omnisocials.errors.AuthenticationException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** API key resolution: explicit key wins, env fallback works, missing key throws at build time. */
class EnvFallbackTest {

  @Test
  void fallsBackToEnvironmentVariable() throws IOException {
    AtomicReference<String> authHeader = new AtomicReference<>();
    try (TestServer server =
        new TestServer(
            exchange -> {
              authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
              TestServer.respond(exchange, 200, "{\"status\":\"ok\"}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .envLookup(name -> "OMNISOCIALS_API_KEY".equals(name) ? "omsk_test_from_env" : null)
              .baseUrl(server.baseUrl())
              .build();
      client.health();

      assertEquals("Bearer omsk_test_from_env", authHeader.get());
    }
  }

  @Test
  void explicitApiKeyWinsOverEnvironment() throws IOException {
    AtomicReference<String> authHeader = new AtomicReference<>();
    AtomicReference<String> userAgent = new AtomicReference<>();
    try (TestServer server =
        new TestServer(
            exchange -> {
              authHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
              userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
              TestServer.respond(exchange, 200, "{\"status\":\"ok\"}");
            })) {
      OmniSocials client =
          OmniSocials.builder()
              .apiKey("omsk_live_explicit")
              .envLookup(name -> "omsk_test_from_env")
              .baseUrl(server.baseUrl())
              .build();
      client.health();

      assertEquals("Bearer omsk_live_explicit", authHeader.get());
      assertEquals("omnisocials-java/" + OmniSocials.VERSION, userAgent.get());
    }
  }

  @Test
  void missingKeyThrowsAuthenticationExceptionAtBuildTime() {
    AuthenticationException e =
        assertThrows(
            AuthenticationException.class,
            () -> OmniSocials.builder().envLookup(name -> null).build());
    assertEquals(401, e.getStatus());
    assertEquals("missing_api_key", e.getCode());
    assertTrue(e.getMessage().contains("OMNISOCIALS_API_KEY"));
    assertTrue(e.getMessage().contains("Settings -> API Keys"));
  }

  @Test
  void emptyEnvValueAlsoThrows() {
    assertThrows(
        AuthenticationException.class, () -> OmniSocials.builder().envLookup(name -> "").build());
  }
}
