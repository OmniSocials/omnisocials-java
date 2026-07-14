package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** media().upload() builds a well-formed multipart/form-data request. */
class MultipartUploadTest {

  @TempDir Path tempDir;

  @Test
  void uploadsBytesAsMultipart() throws IOException {
    byte[] fileBytes = "fake-png-bytes".getBytes(StandardCharsets.UTF_8);
    AtomicReference<String> contentType = new AtomicReference<>();
    AtomicReference<byte[]> requestBody = new AtomicReference<>();

    try (TestServer server =
        new TestServer(
            exchange -> {
              contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
              requestBody.set(exchange.getRequestBody().readAllBytes());
              byte[] response =
                  "{\"data\":{\"id\":\"m_1\",\"name\":\"product\"},\"compatibility\":{}}"
                      .getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, response.length);
              try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
              }
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result =
          client.media().upload(fileBytes, "product.png", Params.of("name", "product"));

      assertEquals("m_1", result.get("data").get("id").asText());
      assertNotNull(contentType.get());
      assertTrue(contentType.get().startsWith("multipart/form-data; boundary="));

      String boundary = contentType.get().substring("multipart/form-data; boundary=".length());
      String body = new String(requestBody.get(), StandardCharsets.UTF_8);
      assertTrue(body.contains("--" + boundary + "\r\n"), "opens with the boundary");
      assertTrue(body.endsWith("--" + boundary + "--\r\n"), "closes the multipart body");
      assertTrue(
          body.contains("Content-Disposition: form-data; name=\"file\"; filename=\"product.png\""),
          "file part present");
      assertTrue(body.contains("fake-png-bytes"), "file bytes present");
      assertTrue(
          body.contains("Content-Disposition: form-data; name=\"name\"\r\n\r\nproduct"),
          "text field present");
    }
  }

  @Test
  void uploadsPathWithFilenameFromDisk() throws IOException {
    Path file = tempDir.resolve("hero.jpg");
    Files.write(file, "jpeg-bytes".getBytes(StandardCharsets.UTF_8));
    AtomicReference<byte[]> requestBody = new AtomicReference<>();

    try (TestServer server =
        new TestServer(
            exchange -> {
              requestBody.set(exchange.getRequestBody().readAllBytes());
              byte[] response = "{\"data\":{\"id\":\"m_2\"}}".getBytes(StandardCharsets.UTF_8);
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, response.length);
              try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
              }
            })) {
      OmniSocials client =
          OmniSocials.builder().apiKey("omsk_test_key").baseUrl(server.baseUrl()).build();

      JsonNode result = client.media().upload(file);

      assertEquals("m_2", result.get("data").get("id").asText());
      String body = new String(requestBody.get(), StandardCharsets.UTF_8);
      assertTrue(body.contains("filename=\"hero.jpg\""), "filename taken from the path");
      assertTrue(body.contains("jpeg-bytes"), "file bytes present");
    }
  }
}
