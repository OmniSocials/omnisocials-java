package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
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
   * {@code GET /analytics/overview?period=&start_date=&end_date=} - workspace
   * totals for a period ({@code 7d}, {@code 30d}, {@code 90d}, or a custom
   * {@code start_date}/{@code end_date} range).
   */
  public JsonNode overview(Map<String, Object> query) {
    return client.get("/analytics/overview", query);
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
