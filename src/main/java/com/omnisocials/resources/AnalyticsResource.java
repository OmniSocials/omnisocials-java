package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.RetryPolicy;
import java.util.List;
import java.util.Map;

/** Post and account analytics. Accessed via {@code client.analytics()}. */
public final class AnalyticsResource extends ApiResource {

  public AnalyticsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /analytics/posts/:id} - latest per-platform metrics for one post. */
  public JsonNode post(String postId) {
    return client.get("/analytics/posts/" + seg(postId));
  }

  /**
   * {@code POST /analytics/posts/:id/refresh} - "Update now": read the X
   * numbers of one published post now, outside its check schedule. X bills
   * every read, so it costs half a credit per tweet (each part of a thread
   * counts, plus half a credit for a video or GIF), and nothing when the post
   * was already read today (UTC). OmniSocials Connect accounts pay on the
   * partner's own X app. At most 5 reads of a post a UTC day. Needs
   * {@code analytics:read}. Returns {@code { data: { post_id, tweets: [{ id,
   * metrics, read_at }], credits_used, read_at } }}. Because it can cost
   * credits it is retried only after a 429 ({@link RetryPolicy#RATE_LIMIT_ONLY}),
   * never after a 5xx, a timeout or a lost connection. Errors: 402
   * {@code insufficient_credits}, 404 {@code not_found}, 409
   * {@code refresh_not_available} (no published X tweet), 429
   * {@code refresh_limit}, 502 {@code refresh_failed} (nothing charged).
   */
  public JsonNode refreshPost(String postId) {
    return client.post("/analytics/posts/" + seg(postId) + "/refresh", RetryPolicy.RATE_LIMIT_ONLY);
  }

  /** {@link #refreshPost(String)} with a numeric post id. */
  public JsonNode refreshPost(long postId) {
    return refreshPost(Long.toString(postId));
  }

  /**
   * {@code GET /analytics/posts?ids=a,b,c} - batch metrics for up to 100
   * posts in one call instead of one request per post.
   */
  public JsonNode posts(List<String> postIds) {
    return client.get("/analytics/posts", Map.of("ids", String.join(",", postIds)));
  }

  /** Varargs convenience for {@link #posts(List)}. */
  public JsonNode posts(String... postIds) {
    return posts(List.of(postIds));
  }

  /** {@code GET /analytics/overview} - workspace-wide totals for the default period. */
  public JsonNode overview() {
    return client.get("/analytics/overview");
  }

  /**
   * {@code GET /analytics/overview?period=&start_date=&end_date=&tag=} -
   * workspace totals for a period ({@code 7d}, {@code 30d}, {@code 90d}, or a
   * custom {@code start_date}/{@code end_date} range). {@code tag} (a tag id
   * or name, comma separated for any of several) limits the post numbers to
   * the posts with that tag; account numbers stay those of the whole account.
   */
  public JsonNode overview(Map<String, Object> query) {
    return client.get("/analytics/overview", query);
  }

  /**
   * {@code GET /analytics/tags} - how each post tag did against your normal
   * over the last 90 days. Returns {@code { data: { period, platform,
   * min_posts, tags, untagged, normals } }}: {@code tags} is a list of
   * comparisons (comparable first, best first), each with {@code tag},
   * {@code posts}, {@code measured}, {@code median_impressions},
   * {@code engagement_rate} (percent), {@code vs_normal} (a fraction, 0.38 =
   * 38% above normal), {@code best_channel} and {@code enough_posts}.
   */
  public JsonNode tags() {
    return client.get("/analytics/tags");
  }

  /**
   * {@code GET /analytics/tags?start_date=&end_date=&platform=&min_posts=} -
   * {@link #tags()} for a period ("YYYY-MM-DD", "YYYY-MM" or ISO 8601), one
   * channel, and the measured posts a tag needs before {@code enough_posts}
   * is true (default 5, 1 to 50).
   */
  public JsonNode tags(Map<String, Object> query) {
    return client.get("/analytics/tags", query);
  }

  /** {@code GET /analytics/accounts} - account-level stats (followers etc). */
  public JsonNode accounts() {
    return client.get("/analytics/accounts");
  }

  /**
   * {@code GET /analytics/accounts?platform=&date=} - account-level stats
   * filtered by platform and/or day.
   */
  public JsonNode accounts(Map<String, Object> query) {
    return client.get("/analytics/accounts", query);
  }

  /**
   * {@code GET /analytics/best-times} - recommended posting time slots for a
   * platform, based on when the account's audience engages. Query params:
   * {@code platform} (required), {@code timezone}.
   */
  public JsonNode bestTimes(Map<String, Object> query) {
    return client.get("/analytics/best-times", query);
  }
}
