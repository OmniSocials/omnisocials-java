package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/** A/B tests and lift studies on Meta. Accessed via {@code client.ads().studies()}. */
public final class AdStudiesResource extends ApiResource {

  public AdStudiesResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /ads/studies?ad_account_id=} - the studies of an ad account. Requires {@code ads:read}. */
  public JsonNode list(long adAccountId) {
    return client.get("/ads/studies", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/studies} - create an A/B test between existing campaigns
   * or ad sets ({@code ad_account_id} and Meta's study fields). Needs a key
   * with the opt-in {@code ads:spend} scope ("Start ads"); without it, and on
   * Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app.
   * Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/studies", params);
  }
}
