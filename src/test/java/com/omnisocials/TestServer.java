package com.omnisocials;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Minimal local HTTP stub for exercising the client against real sockets. */
final class TestServer implements AutoCloseable {

  private final HttpServer server;

  TestServer(HttpHandler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", handler);
    server.start();
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  int port() {
    return server.getAddress().getPort();
  }

  static void respond(HttpExchange exchange, int status, String jsonBody) throws IOException {
    // Drain the request body first so the client never sees a broken pipe.
    exchange.getRequestBody().readAllBytes();
    if (jsonBody == null) {
      exchange.sendResponseHeaders(status, -1);
      exchange.close();
      return;
    }
    byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
    exchange.close();
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
