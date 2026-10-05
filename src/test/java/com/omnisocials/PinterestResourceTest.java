package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Request path, query and response parsing of the Pinterest product endpoints. */
class PinterestResourceTest {

  @Test
  void listProductsSendsTheQueryAndParsesProducts() throws IOException {
    AtomicReference<String> requestLine = new AtomicReference<>();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requestLine.set(
                  exchange.getRequestMethod()
                      + " "
                      + exchange.getRequestURI().getPath()
                      + "?"
                      + exchange.getRequestURI().getRawQuery());
              TestServer.respond(
                  exchange,
                  200,
                  "{\"products\":[{\"pin_id\":\"813744226420795884\",\"title\":\"Blue ribbed top\","
                      + "\"price\":24.99,\"currency\":\"EUR\"}],\"bookmark\":null,"
                      + "\"source\":\"catalog\",\"catalog_access\":true,"
                      + "\"product_groups\":[{\"id\":\"443727193917\",\"name\":\"All Products\"}],"
                      + "\"product_group_id\":\"443727193917\"}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result =
          client
              .pinterest()
              .listProducts(
                  Params.builder()
                      .put("source", "catalog")
                      .put("product_group_id", "443727193917")
                      .put("page_size", 50)
                      .build());

      assertEquals(
          "GET /pinterest/products?source=catalog&product_group_id=443727193917&page_size=50",
          requestLine.get());
      assertEquals("813744226420795884", result.get("products").get(0).get("pin_id").asText());
      assertEquals(24.99, result.get("products").get(0).get("price").asDouble());
      assertTrue(result.get("bookmark").isNull());
      assertTrue(result.get("catalog_access").asBoolean());
      assertEquals("443727193917", result.get("product_group_id").asText());
    }
  }

  @Test
  void listProductsReturnsTheErrorEnvelopeAsIs() throws IOException {
    AtomicReference<String> rawQuery = new AtomicReference<>("unset");
    try (TestServer server =
        new TestServer(
            exchange -> {
              rawQuery.set(exchange.getRequestURI().getRawQuery());
              TestServer.respond(
                  exchange,
                  200,
                  "{\"error\":{\"code\":\"pinterest_not_connected\","
                      + "\"message\":\"No Pinterest account is connected.\"}}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result = client.pinterest().listProducts();

      assertEquals(null, rawQuery.get(), "no query when no params are given");
      assertFalse(result.has("products"));
      assertEquals("pinterest_not_connected", result.get("error").get("code").asText());
    }
  }

  @Test
  void validateProductSendsTheId() throws IOException {
    AtomicReference<String> requestLine = new AtomicReference<>();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requestLine.set(
                  exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getQuery());
              TestServer.respond(
                  exchange,
                  200,
                  "{\"valid\":false,\"pin_id\":\"813744226420795884\",\"unverified\":true,"
                      + "\"reason\":\"Pinterest did not answer.\"}");
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result =
          client.pinterest().validateProduct("https://www.pinterest.com/pin/813744226420795884/");

      assertEquals(
          "/pinterest/products/validate?id=https://www.pinterest.com/pin/813744226420795884/",
          requestLine.get());
      assertFalse(result.get("valid").asBoolean());
      assertTrue(result.get("unverified").asBoolean());
      assertEquals("813744226420795884", result.get("pin_id").asText());
    }
  }
}
