package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.omnisocials.RetryPolicy;
import java.util.Map;

/**
 * Reach and frequency buying on Meta. Accessed via {@code
 * client.ads().reachFrequency()}. A prediction spends nothing; a reservation
 * commits the budget.
 */
public final class AdReachFrequencyResource extends ApiResource {

  public AdReachFrequencyResource(OmniSocials client) {
    super(client);
  }

  private static String prediction(String predictionId) {
    return "/ads/reach-frequency/" + seg(predictionId);
  }

  /**
   * {@code POST /ads/reach-frequency} - create a prediction: {@code
   * ad_account_id}, {@code objective} ({@code awareness}, {@code engagement}
   * or {@code video_views}), {@code targeting} (exactly one country in {@code
   * countries}, optional {@code age_min}, {@code age_max}, {@code genders}),
   * {@code start_at}, {@code end_at}, and {@code budget} (major units) or
   * {@code reach} (people to reach), optional {@code frequency_cap} and {@code
   * frequency_interval_days}. Meta needs the reach and frequency capability on
   * the ad account. Spends nothing. Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/reach-frequency", params);
  }

  /** {@code GET /ads/reach-frequency/:predictionId?ad_account_id=} - read a prediction. Requires {@code ads:read}. */
  public JsonNode get(String predictionId, long adAccountId) {
    return client.get(prediction(predictionId), Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code DELETE /ads/reach-frequency/:predictionId?ad_account_id=} - cancel a
   * prediction or a reservation. Requires {@code ads:write}.
   */
  public JsonNode cancel(String predictionId, long adAccountId) {
    return client.delete(prediction(predictionId), Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/reach-frequency/:predictionId/reserve} - reserve a
   * prediction: it COMMITS REAL AD SPEND. Show the person the prediction's
   * budget and get their yes first; {@code confirmBudgetAmount} must equal it.
   * Needs a key with the opt-in {@code ads:spend} scope ("Start ads"); without
   * it, and on Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app.
   * Not retried after a 5xx or a timeout: read the prediction with
   * {@link #get(String, long)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode reserve(String predictionId, long adAccountId, Number confirmBudgetAmount) {
    return client.post(
        prediction(predictionId) + "/reserve",
        Params.of("ad_account_id", adAccountId, "confirm_budget_amount", confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }
}
