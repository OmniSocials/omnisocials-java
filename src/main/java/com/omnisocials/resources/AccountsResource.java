package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.LinkedHashMap;
import java.util.Map;

/** Connected social accounts. Accessed via {@code client.accounts()}. */
public final class AccountsResource extends ApiResource {

  public AccountsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /accounts} - the workspace's connected social accounts. */
  public JsonNode list() {
    return client.get("/accounts");
  }

  /**
   * {@code GET /accounts/:id} - a single connected account. An X account also
   * carries {@code x_statistics}: posts, followers, post_schedule, source,
   * billing, state and paused_reason (null when it could not be read).
   */
  public JsonNode get(String id) {
    return client.get("/accounts/" + seg(id));
  }

  /**
   * {@code PATCH /accounts/:id} - change how often the X statistics of an X
   * account are read. The body is sent as given, for example
   * {@code { "x_statistics": { "posts": "light", "followers": "daily" } }}
   * (one key or more of posts, followers and post_schedule; a key left out
   * keeps its value). {@code posts} is full (5 checks), light (2 checks) or
   * off; {@code followers} is daily, weekly or off. X bills every read and
   * OmniSocials passes X's price on as credits (half a credit per post check,
   * plus half for a video or GIF, 1 credit per follower count). Needs the
   * opt-in scope {@code accounts:write} and billing rights (manage_billing)
   * for the key's creator. OmniSocials Connect keys (the partner's own X app
   * pays) also take posts custom with {@code post_schedule} ({@code { at:
   * ["1h", "1d"], then_every: "7d", until: "90d" }}), followers hourly / 6h,
   * and {@code x_statistics} null: follow the customer's setting again.
   *
   * <p>Returns {@code { data: account }} with the new {@code x_statistics}.
   * Errors: 400 {@code validation_error} / {@code schedule_invalid} /
   * {@code not_supported} (not an X account), 403 {@code forbidden}, 404
   * {@code not_found}.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/accounts/" + seg(id), params);
  }

  /**
   * {@link #update(String, Map)} with {@code { "x_statistics": xStatistics }}:
   * only the keys of {@code xStatistics} are sent, and {@code null} is sent as
   * null (OmniSocials Connect keys: follow the customer's setting again).
   */
  public JsonNode updateXStatistics(String id, Map<String, Object> xStatistics) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("x_statistics", xStatistics);
    return update(id, body);
  }
}
