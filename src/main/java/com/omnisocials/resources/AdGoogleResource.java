package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Google Ads reports. Accessed via {@code client.ads().google()}. Every call
 * takes {@code ad_account_id} in the query and is a read ({@code ads:read}).
 * Every API caller stops at the draft on Google, also a key with
 * {@code ads:spend} ({@code 403 start_in_app}).
 */
public final class AdGoogleResource extends ApiResource {

  public AdGoogleResource(OmniSocials client) {
    super(client);
  }

  private JsonNode read(String report, Map<String, Object> query) {
    return client.get("/ads/google/" + report, query);
  }

  /**
   * {@code GET /ads/google/search-terms} - the search terms report ({@code
   * campaign_id} or {@code external_campaign_id}, {@code from}, {@code to},
   * {@code limit}).
   */
  public JsonNode searchTerms(Map<String, Object> query) {
    return read("search-terms", query);
  }

  /** {@code GET /ads/google/recommendations} - Google's recommendations ({@code types}). */
  public JsonNode recommendations(Map<String, Object> query) {
    return read("recommendations", query);
  }

  /**
   * {@code GET /ads/google/keywords} - search keywords and negative keywords
   * ({@code external_campaign_id}, {@code external_ad_set_id}).
   */
  public JsonNode keywords(Map<String, Object> query) {
    return read("keywords", query);
  }

  /** {@code GET /ads/google/assets} - sitelinks, callouts and structured snippets ({@code type}). */
  public JsonNode assets(Map<String, Object> query) {
    return read("assets", query);
  }

  /** {@code GET /ads/google/shared-budgets} - shared budgets. */
  public JsonNode sharedBudgets(Map<String, Object> query) {
    return read("shared-budgets", query);
  }

  /** {@code GET /ads/google/bid-strategies} - portfolio bid strategies. */
  public JsonNode bidStrategies(Map<String, Object> query) {
    return read("bid-strategies", query);
  }

  /** {@code GET /ads/google/ad-schedule} - the days and hours of a campaign ({@code external_campaign_id}). */
  public JsonNode adSchedule(Map<String, Object> query) {
    return read("ad-schedule", query);
  }

  /** {@code GET /ads/google/labels} - labels. */
  public JsonNode labels(Map<String, Object> query) {
    return read("labels", query);
  }

  /** {@code GET /ads/google/local-services-leads} - Local Services leads ({@code since}). */
  public JsonNode localServicesLeads(Map<String, Object> query) {
    return read("local-services-leads", query);
  }
}
