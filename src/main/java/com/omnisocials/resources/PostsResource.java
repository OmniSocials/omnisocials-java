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
   * Query params: {@code status} (draft | in_approval | scheduled | posting |
   * published | failed | warning; in_approval = waiting for a reviewer in an
   * approval workflow), {@code limit}, {@code offset}.
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
   * {@code bluesky}, {@code mastodon}, {@code threads}, ...). The same
   * {@code thread_parts} shape (2 to 25 parts) publishes a chained thread on
   * X, Bluesky, Mastodon and Threads. Threads: 500 characters per part, up to
   * 10 media per part; parts after the first publish as replies to the
   * previous part, and the Threads caption is taken from part 1.
   *
   * <p>Threads posts can carry a location tag: {@code location_id} inside the
   * {@code threads} map (an id from {@code GET
   * /locations/search?platform=threads}), or {@code location} ({@code { id,
   * name?, address?, city?, country? }}) to store display fields along with
   * the id; {@code location_id} wins when both are given. On a multi-post
   * thread the tag is applied to part 1, and the Post's {@code threads} block
   * echoes a {@code location} object when set. Threads location tagging is
   * currently rolling out; until Meta approves the permissions it is disabled
   * on production and create/update/publish return a 400 (also a 400
   * {@code validation_error} asking you to reconnect Threads when the
   * connection lacks the {@code threads_location_tagging} permission).
   *
   * <p>Each {@code media_urls} / {@code media_ids} entry is a plain string, or
   * a map with an {@code alt} accessibility description (max 1500 chars):
   * {@code Map.of("url", "https://...", "alt", "...")} for media_urls,
   * {@code Map.of("id", "...", "alt", "...")} for media_ids. Alt text is
   * delivered to Mastodon (media description), Bluesky (embed alt), X
   * (photos/GIFs), Pinterest (pin alt text), Instagram (images), and
   * LinkedIn (images); the same entry shape works inside
   * {@code thread_parts} media.
   *
   * <p>When the post targets X and its text (or any thread part) contains a
   * URL, the response includes a top-level {@code warnings} array (sibling of
   * {@code data}) with a {@code x_url_post_credits} entry carrying
   * {@code credits_required} and {@code credits_balance}: X's link-post fee
   * is passed through as prepaid credits, debited at publish time (from
   * 2026-08-14). Credits are managed in the dashboard, not the API.
   *
   * <p>From 2026-08-14, scheduling an X link post can also be refused up
   * front, before the request is accepted: every scheduled X link post
   * reserves its cost, and if reserving this one would push the company's
   * total reserved credits past its balance, this call throws an
   * {@link com.omnisocials.errors.ApiException} with status 402 and code
   * {@code x_credits_insufficient}, whose body carries
   * {@code error.details.credits_required}, {@code credits_balance}, and
   * {@code credits_reserved}. Drafts (no {@code scheduled_at}) are never
   * gated, and posts scheduled to publish before 2026-08-14 are never gated.
   * The same gate applies to {@link #update(String, Map)} and
   * {@link #publish(String)}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/posts/create", params);
  }

  /**
   * {@code POST /posts/create-and-publish} - create a post and publish it immediately.
   * See {@link #create(Map)} for the {@code warnings} array on X link posts
   * and the X link-post credit gate (402 {@code x_credits_insufficient}).
   */
  public JsonNode createAndPublish(Map<String, Object> params) {
    return client.post("/posts/create-and-publish", params);
  }

  /**
   * {@code PATCH /posts/:id} - update a draft or scheduled post. Pass
   * {@code thread_parts: null} inside a platform options map ({@code x},
   * {@code bluesky}, {@code mastodon}, {@code threads}) to clear thread
   * mode; omit it to leave the existing thread untouched. A Threads location
   * tag clears the same way: {@code location_id: null} (or
   * {@code location: null}) inside {@code threads} removes the tag, while
   * omitting the key leaves it untouched.
   *
   * <p>See {@link #create(Map)} for the X link-post credit gate (402
   * {@code x_credits_insufficient}), which also applies here when the update
   * (re)schedules an X post whose text contains a URL.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/posts/" + seg(id), params);
  }

  /** {@code DELETE /posts/:id} - delete a post. Returns {@code null} (204). */
  public JsonNode delete(String id) {
    return client.delete("/posts/" + seg(id));
  }

  /**
   * {@code POST /posts/:id/publish} - publish a draft or scheduled post now.
   *
   * <p>See {@link #create(Map)} for the X link-post credit gate (402
   * {@code x_credits_insufficient}), which also applies here.
   */
  public JsonNode publish(String id) {
    return client.post("/posts/" + seg(id) + "/publish");
  }

  /**
   * {@code POST /posts/:id/retry} - retry the failed platforms of a
   * {@code failed} or {@code warning} (partially failed) post, on the same
   * post. Only the platforms that failed are re-published; platforms that
   * already succeeded are never posted again. Asynchronous: a 200 means the
   * retry is queued - poll {@code get(id)} for the outcome. Max 3 retries per
   * platform.
   */
  public JsonNode retry(String id) {
    return client.post("/posts/" + seg(id) + "/retry");
  }
}
