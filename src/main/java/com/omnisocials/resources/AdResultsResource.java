package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Your own numbers per campaign and day: qualified leads, sales, revenue.
 * Accessed via {@code client.ads().results()}. Rules read them as {@code
 * results.<metric>}, formulas as {@code formula.<key>}.
 */
public final class AdResultsResource extends ApiResource {

  public AdResultsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /ads/campaign-results} - your numbers (query {@code from},
   * {@code to}, {@code campaign_id}, {@code metric}; may be null). Requires
   * {@code ads:read}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/ads/campaign-results", query);
  }

  /**
   * {@code POST /ads/campaign-results} - upload {@code rows} (each {@code day},
   * {@code metric}, {@code value} and the campaign as {@code campaign_id},
   * {@code external_campaign_id} + {@code platform}, {@code utm_campaign} or
   * {@code campaign_name}; at most 5,000). Sending a day again replaces it.
   * Requires {@code ads:write}.
   */
  public JsonNode upload(Map<String, Object> params) {
    return client.post("/ads/campaign-results", params);
  }

  /**
   * {@code GET /ads/campaign-results/totals} - totals over a period with the
   * formulas (query {@code from}, {@code to}, {@code campaign_id}; may be
   * null). Requires {@code ads:read}.
   */
  public JsonNode totals(Map<String, Object> query) {
    return client.get("/ads/campaign-results/totals", query);
  }

  /** {@code GET /ads/campaign-results/metrics} - your metric names and formulas. Requires {@code ads:read}. */
  public JsonNode metrics() {
    return client.get("/ads/campaign-results/metrics");
  }

  /**
   * {@code PUT /ads/campaign-results/metrics/:key} - name a metric ({@code
   * label}, {@code unit} {@code number}/{@code money}/{@code percent}) or
   * define a {@code formula} (for example {@code results_revenue / spend}).
   * Requires {@code ads:write}.
   */
  public JsonNode putMetric(String key, Map<String, Object> params) {
    return client.put("/ads/campaign-results/metrics/" + seg(key), params);
  }
}
