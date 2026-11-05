package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Feature requests to the OmniSocials team, for the person who owns the key
 * (their name and email are stored with it so the team can answer).
 * Accessed via {@code client.featureRequests()}. The person sees them in
 * OmniSocials (Settings, Request a feature) and gets an email when the status
 * changes. No scope needed.
 *
 * <p>A request is {@code { id, title, summary, status (open, in_progress,
 * completed, declined), team_answer, team_replies: [{ message, created_at }],
 * votes_count, limit_id, platform, area, source, created_at, updated_at,
 * completed_at }}.
 *
 * <p>Error codes: {@code invalid_summary}, {@code invalid_title},
 * {@code invalid_platform}, {@code invalid_area}, {@code unknown_limit},
 * {@code invalid_id} (400); {@code not_found} (404); {@code not_requestable}
 * (409: the {@code limit_id} is a platform rule or a choice of OmniSocials,
 * the entry is in {@code error.limit} of {@code getBody()} and nothing was
 * stored); {@code too_many_requests} (429, 10 an hour per person).
 */
public final class FeatureRequestsResource extends ApiResource {

  public FeatureRequestsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code POST /feature-requests} - send a request. Send it only when the
   * person asked for it. Params: {@code summary} (required, 10 to 1000
   * characters: what the person wants to do and why), {@code title} (at most
   * 120, made from the summary when left out), {@code platform},
   * {@code area}, {@code limit_id} (the {@code id} of the
   * {@link LimitsResource#list()} entry the request is about). Returns
   * {@code { outcome, data }}: {@code outcome} is {@code created} (a new
   * request), {@code voted} (a vote added to the organisation's open request
   * for the same gap) or {@code already_requested} (this person already has
   * an open request for it; {@code data} is that request).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/feature-requests", params);
  }

  /**
   * {@code GET /feature-requests} - the person's own requests, newest first,
   * at most 50: {@code { data: FeatureRequest[] }}.
   */
  public JsonNode list() {
    return client.get("/feature-requests");
  }

  /** {@code GET /feature-requests/:id} - one request with its status and the team's replies. */
  public JsonNode get(String id) {
    return client.get("/feature-requests/" + seg(id));
  }

  /** {@link #get(String)} with a numeric request id. */
  public JsonNode get(long id) {
    return get(Long.toString(id));
  }
}
