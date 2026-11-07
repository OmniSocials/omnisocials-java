package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Targeting options of a network. Accessed via {@code
 * client.ads().targeting()}. The ids go into {@code
 * targeting.networks.<network>.include} / {@code exclude} of a draft, or
 * {@code targeting.network} of a campaign; {@link AdsResource#capabilities()}
 * lists the {@code targeting_types} per network. All four calls are reads
 * ({@code ads:read}).
 */
public final class AdTargetingResource extends ApiResource {

  public AdTargetingResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /ads/targeting/search} - search interests, places, languages,
   * job titles and more. Query: {@code ad_account_id}, {@code type}, {@code q},
   * optional {@code limit} (up to 100) and {@code country}. Answers {@code
   * items} with {@code id}, {@code name}, {@code type}, {@code path} and the
   * audience size range.
   */
  public JsonNode search(Map<String, Object> query) {
    return client.get("/ads/targeting/search", query);
  }

  /**
   * {@code GET /ads/targeting/browse} - the targeting categories of one {@code
   * type} as a tree (query {@code ad_account_id}, {@code type}).
   */
  public JsonNode browse(Map<String, Object> query) {
    return client.get("/ads/targeting/browse", query);
  }

  /**
   * {@code POST /ads/targeting/reach-estimate} - the audience size of a
   * targeting ({@code ad_account_id}, {@code targeting} with {@code
   * countries}, {@code age_min}, {@code age_max}, {@code genders}, {@code
   * network}, optional {@code objective}). Answers {@code lower}, {@code
   * upper}, {@code daily}.
   */
  public JsonNode reachEstimate(Map<String, Object> params) {
    return client.post("/ads/targeting/reach-estimate", params);
  }

  /**
   * {@code POST /ads/targeting/bid-suggestions} - a suggested bid and daily
   * budget ({@code ad_account_id}, {@code targeting}, optional {@code
   * objective} and {@code budget}). Spends nothing.
   */
  public JsonNode bidSuggestions(Map<String, Object> params) {
    return client.post("/ads/targeting/bid-suggestions", params);
  }
}
