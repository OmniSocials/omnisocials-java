package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Long reports that run in the background. Accessed via {@code
 * client.ads().reports()}. All calls are reads ({@code ads:read}).
 */
public final class AdReportsResource extends ApiResource {

  public AdReportsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /ads/insights/reports} - the background reports of the workspace. */
  public JsonNode list() {
    return client.get("/ads/insights/reports");
  }

  /**
   * {@code POST /ads/insights/reports} - start a long report (up to 730 days)
   * with the body of {@link AdInsightsResource#query(Map)}. Answers 202; poll
   * it with {@link #get(String)}.
   */
  public JsonNode start(Map<String, Object> params) {
    return client.post("/ads/insights/reports", params);
  }

  /**
   * {@code GET /ads/insights/reports/:reportId} - poll a report: {@code
   * status} {@code running}, {@code done} (with {@code rows}) or {@code
   * failed}.
   */
  public JsonNode get(String reportId) {
    return client.get("/ads/insights/reports/" + seg(reportId));
  }
}
