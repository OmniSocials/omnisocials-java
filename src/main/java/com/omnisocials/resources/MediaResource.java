package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.errors.OmniSocialsException;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** The media library. Accessed via {@code client.media()}. */
public final class MediaResource extends ApiResource {

  public MediaResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /media} - list the media library (newest first). */
  public JsonNode list() {
    return client.get("/media");
  }

  /**
   * {@code GET /media?limit=&offset=&search=&folder_id=} - list with filters.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/media", query);
  }

  /** {@code GET /media/:id} - fetch a single media item. */
  public JsonNode get(String id) {
    return client.get("/media/" + seg(id));
  }

  /**
   * {@code POST /media/upload} - upload raw bytes as multipart form data (max
   * 50MB for images, 100MB request cap overall; use {@link #uploadFromUrl} or
   * {@link #createUploadUrl} for larger videos, up to 1GB).
   *
   * <p>A PDF is rasterized into image slides and returned as a carousel
   * ({@code slides} + {@code media_ids}).
   */
  public JsonNode upload(byte[] file, String filename) {
    return upload(file, filename, null);
  }

  /**
   * {@code POST /media/upload} - upload raw bytes with extra form fields.
   * Supported params: {@code name}, {@code folder}, {@code folder_id}.
   */
  public JsonNode upload(byte[] file, String filename, Map<String, Object> params) {
    String contentType = filename == null ? null : URLConnection.guessContentTypeFromName(filename);
    return client.postMultipart("/media/upload", file, filename, contentType, params);
  }

  /** {@code POST /media/upload} - upload a file from disk (filename taken from the path). */
  public JsonNode upload(Path file) {
    return upload(file, null);
  }

  /**
   * {@code POST /media/upload} - upload a file from disk with extra form
   * fields. Supported params: {@code name}, {@code folder}, {@code folder_id}.
   */
  public JsonNode upload(Path file, Map<String, Object> params) {
    byte[] bytes;
    String contentType;
    try {
      bytes = Files.readAllBytes(file);
      contentType = Files.probeContentType(file);
    } catch (IOException e) {
      throw new OmniSocialsException("Failed to read file for upload: " + file, e);
    }
    String filename = file.getFileName() == null ? "upload.bin" : file.getFileName().toString();
    if (contentType == null) {
      contentType = URLConnection.guessContentTypeFromName(filename);
    }
    return client.postMultipart("/media/upload", bytes, filename, contentType, params);
  }

  /**
   * {@code POST /media/upload-from-url} - the server fetches a public URL
   * (files up to 1GB; large videos finish processing in the background and
   * come back with status "processing"). Params: {@code url} (required),
   * {@code name}, {@code folder}.
   */
  public JsonNode uploadFromUrl(Map<String, Object> params) {
    return client.post("/media/upload-from-url", params);
  }

  /**
   * {@code POST /media/upload-from-base64} - upload base64-encoded file data.
   * Params: {@code data} (no data URI prefix), {@code mime_type},
   * {@code filename}, {@code name}, {@code folder}.
   */
  public JsonNode uploadFromBase64(Map<String, Object> params) {
    return client.post("/media/upload-from-base64", params);
  }

  /**
   * {@code POST /media/create-upload-url} - mint a one-time presigned upload
   * URL for large files (up to 1GB). POST the file as multipart form data
   * (field name "file") to the returned {@code upload_url} within
   * {@code expires_in_seconds}; no auth headers are needed on that second
   * request.
   */
  public JsonNode createUploadUrl() {
    return client.post("/media/create-upload-url");
  }

  /**
   * {@code POST /media/check} - preflight a file's compatibility with the
   * workspace's connected platforms BEFORE uploading. Provide one of: a
   * public {@code url}, an existing {@code media_id}, or {@code size_bytes} +
   * {@code mime}.
   */
  public JsonNode check(Map<String, Object> params) {
    return client.post("/media/check", params);
  }

  /**
   * {@code PATCH /media/:id} - rename a file and/or move it into a folder.
   * Params: {@code name}, {@code folder_id}.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/media/" + seg(id), params);
  }

  /**
   * {@code DELETE /media/:id} - delete a media file. Returns {@code null}
   * (204). Fails with 409 {@code media_in_use} when the file is attached to a
   * scheduled or publishing post.
   */
  public JsonNode delete(String id) {
    return client.delete("/media/" + seg(id));
  }
}
