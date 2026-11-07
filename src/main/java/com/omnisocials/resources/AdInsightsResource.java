package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Numbers: live numbers from the network, campaign and ad numbers, the daily
 * timeline, rankings and alerts. Accessed via {@code client.ads().insights()};
 * long reports that run in the background are in {@code
 * client.ads().reports()}. Money is in major units; {@code ctr} is a percent.
 * All calls are reads ({@code ads:read}).
 */
public final class AdInsightsResource extends ApiResource {

  public AdInsightsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code POST /ads/insights} - live numbers: {@code ad_account_id}, {@code
   * level} ({@code account}, {@code campaign}, {@code ad_set}, {@code ad}),
   * {@code ids}, {@code metrics}, up to 2 {@code breakdowns}, {@code from},
   * {@code to}, {@code time_increment} ({@code day} or {@code all}). Answers
   * {@code rows}.
   */
  public JsonNode query(Map<String, Object> params) {
    return client.post("/ads/insights", params);
  }

  /**
   * {@code GET /ads/campaigns/:id/analytics} - numbers of an OmniSocials
   * campaign. Query (may be null): {@code from}, {@code to}, {@code breakdown}
   * ({@code ad} gives one row per ad, or {@code country}, {@code age}, {@code
   * gender}, {@code placement}, {@code device}, {@code platform}), {@code
   * time_increment}, {@code metrics} (a list or a comma list).
   */
  public JsonNode campaign(long campaignId, Map<String, Object> query) {
    return client.get("/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/analytics", query);
  }

  /**
   * {@code GET /ads/campaigns/:id/ads/:adId/analytics} - numbers of one ad
   * (query {@code from}, {@code to}, {@code breakdown}; may be null).
   */
  public JsonNode ad(long campaignId, long adId, Map<String, Object> query) {
    return client.get(
        "/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/ads/" + seg(String.valueOf(adId))
            + "/analytics",
        query);
  }

  /**
   * {@code GET /ads/timeline} - daily totals of the workspace, OmniSocials and
   * imported campaigns (query {@code from}, {@code to}, {@code platform},
   * {@code source}; may be null).
   */
  public JsonNode timeline(Map<String, Object> query) {
    return client.get("/ads/timeline", query);
  }

  /**
   * {@code GET /ads/performance} - campaigns or ads ranked by one metric
   * (query {@code level}, {@code metric}, {@code order}, {@code limit}, {@code
   * min_impressions}, {@code from}, {@code to}, {@code platform}; may be null).
   */
  public JsonNode performance(Map<String, Object> query) {
    return client.get("/ads/performance", query);
  }

  /**
   * {@code GET /ads/alerts} - what needs attention: rejected ads, held
   * campaigns, sign-in or payment problems, no delivery.
   */
  public JsonNode alerts() {
    return client.get("/ads/alerts");
  }
}
