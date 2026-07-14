package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Posts: drafts, scheduled, and published. Accessed via {@code client.posts()}. */
public final class PostsResource extends ApiResource {

  public PostsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /posts} - list posts in the workspace (newest first). */
  public JsonNode list() {
    return client.get("/posts");
  }

  /**
   * {@code GET /posts?status=&limit=&offset=} - list posts with filters.
   * Query params: {@code status} (draft | scheduled | published | failed),
   * {@code limit}, {@code offset}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/posts", query);
  }

  /** {@code GET /posts/:id} - fetch a single post. */
  public JsonNode get(String id) {
    return client.get("/posts/" + seg(id));
  }

  /**
   * {@code GET /posts/recent-platform} - recent posts fetched live from the
   * connected platform APIs (including content published outside
   * OmniSocials). The fallback for brand-new workspaces where {@code list()}
   * is empty. Requires the {@code analytics:read} scope.
   */
  public JsonNode recentPlatform() {
    return client.get("/posts/recent-platform");
  }

  /**
   * {@code GET /posts/recent-platform?limit=&platforms=} - query params:
   * {@code limit}, {@code platforms} (a {@code List} or comma-separated string).
   */
  public JsonNode recentPlatform(Map<String, Object> query) {
    return client.get("/posts/recent-platform", query);
  }

  /**
   * {@code POST /posts/create} - create a draft or scheduled post.
   *
   * <p>Params: {@code content} (string, or a map of per-platform captions with
   * a {@code default} key), {@code channels} (list of platform ids),
   * {@code scheduled_at} (ISO 8601; omit for a draft), {@code media_urls},
   * {@code media_ids}, {@code location_id}, plus per-platform option maps
   * ({@code instagram}, {@code youtube}, {@code x} with {@code thread_parts},
   * {@code bluesky}, {@code mastodon}, ...).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/posts/create", params);
  }

  /** {@code POST /posts/create-and-publish} - create a post and publish it immediately. */
  public JsonNode createAndPublish(Map<String, Object> params) {
    return client.post("/posts/create-and-publish", params);
  }

  /**
   * {@code PATCH /posts/:id} - update a draft or scheduled post. Pass
   * {@code thread_parts: null} inside a platform options map to clear thread
   * mode; omit it to leave the existing thread untouched.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/posts/" + seg(id), params);
  }

  /** {@code DELETE /posts/:id} - delete a post. Returns {@code null} (204). */
  public JsonNode delete(String id) {
    return client.delete("/posts/" + seg(id));
  }

  /** {@code POST /posts/:id/publish} - publish a draft or scheduled post now. */
  public JsonNode publish(String id) {
    return client.post("/posts/" + seg(id) + "/publish");
  }
}
