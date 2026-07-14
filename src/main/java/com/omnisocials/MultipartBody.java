package com.omnisocials;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Hand-built {@code multipart/form-data} request body (no external
 * dependencies). Internal to the SDK.
 */
final class MultipartBody {

  final String boundary;
  final byte[] bytes;

  private MultipartBody(String boundary, byte[] bytes) {
    this.boundary = boundary;
    this.bytes = bytes;
  }

  String contentType() {
    return "multipart/form-data; boundary=" + boundary;
  }

  /**
   * Build a multipart body with one file part (field name "file") plus
   * optional text fields. Null field values are skipped.
   */
  static MultipartBody build(
      byte[] fileBytes, String filename, String fileContentType, Map<String, Object> fields) {
    String boundary = "----omnisocials-" + UUID.randomUUID();
    ByteArrayOutputStream out = new ByteArrayOutputStream(fileBytes.length + 1024);
    try {
      if (fields != null) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
          if (entry.getValue() == null) {
            continue;
          }
          writeAscii(out, "--" + boundary + "\r\n");
          writeAscii(
              out,
              "Content-Disposition: form-data; name=\"" + quote(entry.getKey()) + "\"\r\n\r\n");
          out.write(String.valueOf(entry.getValue()).getBytes(StandardCharsets.UTF_8));
          writeAscii(out, "\r\n");
        }
      }

      writeAscii(out, "--" + boundary + "\r\n");
      writeAscii(
          out,
          "Content-Disposition: form-data; name=\"file\"; filename=\""
              + quote(filename == null ? "upload.bin" : filename)
              + "\"\r\n");
      writeAscii(
          out,
          "Content-Type: "
              + (fileContentType == null ? "application/octet-stream" : fileContentType)
              + "\r\n\r\n");
      out.write(fileBytes);
      writeAscii(out, "\r\n--" + boundary + "--\r\n");
    } catch (IOException e) {
      // ByteArrayOutputStream never throws; keep the compiler happy.
      throw new UncheckedIOException(e);
    }
    return new MultipartBody(boundary, out.toByteArray());
  }

  private static void writeAscii(ByteArrayOutputStream out, String value) throws IOException {
    out.write(value.getBytes(StandardCharsets.UTF_8));
  }

  /** Escape quotes/newlines in header values per RFC 7578 percent-encoding style. */
  private static String quote(String value) {
    return value.replace("\r", "%0D").replace("\n", "%0A").replace("\"", "%22");
  }
}
