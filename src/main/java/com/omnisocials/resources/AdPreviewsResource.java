package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.List;
import java.util.Map;

/**
 * Previews drawn by the network. Accessed via {@code client.ads().previews()}.
 * Spends nothing; both calls are reads ({@code ads:read}).
 */
public final class AdPreviewsResource extends ApiResource {

  public AdPreviewsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code POST /ads/previews} - previews of a new ad: {@code ad_account_id},
   * {@code ad} (the shape of a draft's ad), optional {@code placements} and
   * {@code objective}. Answers {@code previews} ({@code placement}, {@code
   * html}, {@code url}).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/previews", params);
  }

  /** {@code GET /ads/campaigns/:id/ads/:adId/previews} - previews of an existing ad, all placements. */
  public JsonNode forAd(long campaignId, long adId) {
    return forAd(campaignId, adId, null);
  }

  /**
   * {@code GET /ads/campaigns/:id/ads/:adId/previews} - previews of an existing
   * ad, optionally for some {@code placements} (null or empty = all).
   */
  public JsonNode forAd(long campaignId, long adId, List<String> placements) {
    return client.get(
        "/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/ads/" + seg(String.valueOf(adId))
            + "/previews",
        placements == null || placements.isEmpty() ? null : Params.of("placements", placements));
  }
}
