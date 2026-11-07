package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.omnisocials.RetryPolicy;
import java.util.List;
import java.util.Map;

/**
 * The ads of an OmniSocials campaign: list, add, read, pause or switch on, and
 * the network's review. Accessed via {@code client.ads().campaignAds()}.
 */
public final class AdCampaignAdsResource extends ApiResource {

  public AdCampaignAdsResource(OmniSocials client) {
    super(client);
  }

  private static String ads(long campaignId) {
    return "/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/ads";
  }

  private static String ad(long campaignId, long adId) {
    return ads(campaignId) + "/" + seg(String.valueOf(adId));
  }

  /**
   * {@code GET /ads/campaigns/:id/ads} - the ads of an OmniSocials campaign
   * with their status, review and latest numbers. Requires {@code ads:read}.
   */
  public JsonNode list(long campaignId) {
    return client.get(ads(campaignId));
  }

  /**
   * {@code POST /ads/campaigns/:id/ads} - add up to 6 new ads to a campaign
   * that runs, in the shape of a draft's ads ({@code primary_text},
   * {@code headline}, {@code media}, {@code cta}, {@code link},
   * {@code network}). The new ads are created PAUSED on the network; switch
   * each one on with {@link #setStatus(long, long, String, Number)}. Adding ads
   * to a running campaign needs a key with the opt-in {@code ads:spend} scope
   * ("Start ads"); without it, and on Google ({@code api_draft_only}) always,
   * the call answers {@code 403 start_in_app} with the page where a person does
   * it in the app. Webhook {@code ad.created}. Requires {@code ads:write}.
   */
  public JsonNode add(long campaignId, List<Map<String, Object>> ads) {
    return client.post(ads(campaignId), Params.of("ads", ads));
  }

  /** {@code GET /ads/campaigns/:id/ads/:adId} - one ad of an OmniSocials campaign. Requires {@code ads:read}. */
  public JsonNode get(long campaignId, long adId) {
    return client.get(ad(campaignId, adId));
  }

  /**
   * {@code PUT /ads/campaigns/:id/ads/:adId/status} - pause ({@code paused}) or
   * switch on ({@code active}) one ad. Switching on STARTS REAL AD SPEND: show
   * the person the campaign budget and get their yes first, then pass it as
   * {@code confirmBudgetAmount}. Switching on needs a key with the opt-in
   * {@code ads:spend} scope ("Start ads"); without it, and on Google
   * ({@code api_draft_only}) always, the call answers {@code 403 start_in_app}
   * with the page where a person does it in the app. Pausing needs no amount
   * (pass {@code null}) and stays open (Google: {@code start_in_app}). Webhook
   * {@code ad.updated}. Not retried after a 5xx or a timeout: read the ad with
   * {@link #get(long, long)} to see what happened. Requires {@code ads:write}.
   */
  public JsonNode setStatus(long campaignId, long adId, String status, Number confirmBudgetAmount) {
    return client.put(
        ad(campaignId, adId) + "/status",
        AdObjectsResource.statusBody(status, confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code GET /ads/campaigns/:id/ads/:adId/review} - the network's review
   * verdict of one ad: {@code in_review}, {@code approved} or {@code rejected}
   * with reasons. Requires {@code ads:read}.
   */
  public JsonNode review(long campaignId, long adId) {
    return client.get(ad(campaignId, adId) + "/review");
  }
}
