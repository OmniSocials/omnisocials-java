package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/**
 * Conversion tracking: pixels and datasets, the events a campaign can
 * optimise for, custom conversions and events sent from your server.
 * Accessed via {@code client.ads().conversions()}.
 */
public final class AdConversionsResource extends ApiResource {

  public AdConversionsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /ads/conversions/destinations?ad_account_id=} - pixels,
   * datasets, conversion tags and conversion actions. Requires {@code
   * ads:read}.
   */
  public JsonNode destinations(long adAccountId) {
    return client.get("/ads/conversions/destinations", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code GET /ads/conversions/goals?ad_account_id=} - the conversion events
   * a campaign can optimise for ({@code settings.optimization.conversion}).
   * Requires {@code ads:read}.
   */
  public JsonNode goals(long adAccountId) {
    return client.get("/ads/conversions/goals", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/conversions/custom} - create a custom conversion: {@code
   * ad_account_id}, {@code name}, {@code destination_id} and an {@code event}
   * and/or {@code url_contains} rule. Requires {@code ads:write}.
   */
  public JsonNode createCustom(Map<String, Object> params) {
    return client.post("/ads/conversions/custom", params);
  }

  /**
   * {@code POST /ads/conversions/events} - send conversion events from your
   * server: {@code ad_account_id}, {@code destination_id} and up to 1,000
   * {@code events} ({@code event_name}, {@code event_time}, {@code event_id},
   * {@code action_source}, {@code value}, {@code currency}, {@code user} ...).
   * Send the raw email and phone: OmniSocials normalizes and SHA-256 hashes
   * them before they leave and stores nothing. Requires {@code ads:write}.
   */
  public JsonNode sendEvents(Map<String, Object> params) {
    return client.post("/ads/conversions/events", params);
  }
}
