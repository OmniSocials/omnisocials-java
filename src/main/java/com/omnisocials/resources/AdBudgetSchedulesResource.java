package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.RetryPolicy;
import java.util.Map;

/**
 * Planned budget increases of a campaign for high-demand periods on Meta.
 * Accessed via {@code client.ads().budgetSchedules()}.
 */
public final class AdBudgetSchedulesResource extends ApiResource {

  public AdBudgetSchedulesResource(OmniSocials client) {
    super(client);
  }

  private static String schedules(long campaignId) {
    return "/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/budget-schedules";
  }

  /** {@code GET /ads/campaigns/:id/budget-schedules} - the planned increases. Requires {@code ads:read}. */
  public JsonNode list(long campaignId) {
    return client.get(schedules(campaignId));
  }

  /**
   * {@code POST /ads/campaigns/:id/budget-schedules} - plan a budget increase:
   * {@code start_at}, {@code end_at}, {@code budget_increase} ({@code type}
   * {@code absolute} = the extra per day, or {@code multiplier} = a whole
   * percentage on top, 50 = 50% more; {@code value}) and
   * {@code confirm_budget_amount}. It is NEW AD SPEND in that period: show the
   * person the amount and get their yes first; {@code confirm_budget_amount} =
   * the daily total during the period (the daily budget plus the increase),
   * under the OmniSocials daily cap. Needs a key with the opt-in
   * {@code ads:spend} scope ("Start ads"); without it, and on Google
   * ({@code api_draft_only}) always, the call answers {@code 403 start_in_app}
   * with the page where a person does it in the app. Not retried after a 5xx or
   * a timeout: read {@link #list(long)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode create(long campaignId, Map<String, Object> params) {
    return client.post(schedules(campaignId), params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code DELETE /ads/campaigns/:id/budget-schedules/:scheduleId} - remove a
   * planned increase. Requires {@code ads:write}.
   */
  public JsonNode delete(long campaignId, String scheduleId) {
    return client.delete(schedules(campaignId) + "/" + seg(scheduleId));
  }
}
