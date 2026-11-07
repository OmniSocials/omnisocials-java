package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.omnisocials.RetryPolicy;
import java.util.Map;

/**
 * "Create an ad" drafts. Accessed via {@code client.ads().drafts()}.
 *
 * <p>A draft group holds one campaign per network in {@code platforms}, each
 * with the same ads. A draft never spends; a person starts it in the app from
 * its {@code review_url}, or {@link #launch(String, Number)} starts it with a
 * key that has the opt-in {@code ads:spend} scope.
 */
public final class AdDraftsResource extends ApiResource {

  public AdDraftsResource(OmniSocials client) {
    super(client);
  }

  private static String draft(String groupId) {
    return "/ads/drafts/" + seg(groupId);
  }

  /**
   * {@code GET /ads/drafts/defaults} - suggested countries (where the
   * Instagram followers are), the currency and the workspace's ad account per
   * network. Requires {@code ads:read}.
   */
  public JsonNode defaults() {
    return client.get("/ads/drafts/defaults");
  }

  /**
   * {@code POST /ads/drafts} - save a new ad as a draft group. It needs no ad
   * account and spends nothing. Params: {@code objective}, {@code platforms}
   * (network keys, or maps of {@code platform}, {@code budget_amount},
   * {@code ad_account_id}), {@code budget} ({@code amount}, {@code currency}:
   * the daily amount per network), {@code targeting} ({@code countries},
   * {@code automatic_audience}, ...), {@code ads}, optional {@code name} and
   * {@code schedule} ({@code duration_days}; null = until it is stopped).
   * Read {@link AdsResource#capabilities()} first for what each network
   * accepts. The answer carries {@code review_url} (the builder in the
   * dashboard, where the person checks the ad and starts it) and per network
   * {@code readiness}. A {@code budget.currency} that differs from the
   * currency of an attached ad account answers {@code 400 currency_mismatch}
   * with {@code param: "budget.currency"}. Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/drafts", params);
  }

  /** {@code GET /ads/drafts/:group_id} - one draft group. Requires {@code ads:read}. */
  public JsonNode get(String groupId) {
    return client.get(draft(groupId));
  }

  /**
   * {@code PUT /ads/drafts/:group_id} - replace the draft with the full body
   * (the same params as {@link #create(Map)}). Networks that already started
   * are left alone ({@code campaigns[].editable}). A {@code budget.currency}
   * that differs from the currency of an attached ad account answers
   * {@code 400 currency_mismatch} with {@code param: "budget.currency"}.
   * Requires {@code ads:write}.
   */
  public JsonNode update(String groupId, Map<String, Object> params) {
    return client.put(draft(groupId), params);
  }

  /**
   * {@code DELETE /ads/drafts/:group_id} - delete a draft group that never
   * started. Requires {@code ads:write}.
   */
  public JsonNode delete(String groupId) {
    return client.delete(draft(groupId));
  }

  /**
   * {@code POST /ads/drafts/:group_id/account} - set the ad account of one
   * network in the draft (an account from {@link AdsResource#listAccounts()},
   * same network). Requires {@code ads:write}.
   */
  public JsonNode attachAccount(String groupId, String platform, long adAccountId) {
    return client.post(
        draft(groupId) + "/account", Params.of("platform", platform, "ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/drafts/:group_id/launch} - START REAL AD SPEND for every
   * network of the draft that never started. Needs a key with the opt-in
   * {@code ads:spend} scope ("Start ads"): without it the call answers
   * {@code 403 start_in_app} with the draft's page (its {@code review_url},
   * hand that to the person instead). A network in {@code api_draft_only} on
   * {@link AdsResource#capabilities()} (Google) is skipped with code
   * {@code start_in_app}. Show the person the daily amount per network and get
   * their yes first; {@code confirmBudgetAmount} must equal the daily amount of
   * every network. A wrong amount answers
   * {@code 400 budget_confirmation_required} and writes nothing. Answers
   * {@code { data: { group, results } }}, one result per network
   * ({@code campaign_id} is a string); one failed network never stops the
   * others. Not retried after a 5xx or a timeout: read the draft with
   * {@link #get(String)} to see what started. Requires {@code ads:write}.
   */
  public JsonNode launch(String groupId, Number confirmBudgetAmount) {
    return launch(groupId, Params.of("confirm_budget_amount", confirmBudgetAmount));
  }

  /**
   * {@code POST /ads/drafts/:group_id/launch} - START REAL AD SPEND, with the
   * confirmation as params: {@code confirm_budget_amount} (one daily amount for
   * every network) or {@code confirm_budget_amounts} (a map of campaign id to
   * its daily amount). Needs a key with {@code ads:spend}, like
   * {@link #launch(String, Number)}: without it the call answers
   * {@code 403 start_in_app} with the draft's page; a network in
   * {@code api_draft_only} (Google) is skipped with code {@code start_in_app}.
   * Show the person the amounts and get their yes first. Not retried after a
   * 5xx or a timeout: read the draft with {@link #get(String)} to see what
   * started. Requires {@code ads:write}.
   */
  public JsonNode launch(String groupId, Map<String, Object> params) {
    return client.post(draft(groupId) + "/launch", params, RetryPolicy.RATE_LIMIT_ONLY);
  }
}
