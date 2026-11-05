package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.omnisocials.RetryPolicy;
import java.util.List;
import java.util.Map;

/**
 * Ads on Meta, Pinterest, TikTok, LinkedIn, X and Google. Accessed via
 * {@code client.ads()}; "Create an ad" drafts via {@code client.ads().drafts()}.
 *
 * <p>Ads spend real money once a campaign starts. Boosts, campaigns and drafts
 * are created as drafts (no spend). {@link #activate(long, Number)},
 * {@link #activateMulti(List, Number)}, {@link #resume(long)},
 * {@link AdDraftsResource#launch(String, Number)} and a budget raise through
 * {@link #update(long, Map)} start or raise spend; the activations and the
 * launch take the amount again as a confirmation. These calls are retried on a
 * 429 only, never after a 5xx or a timeout: the first attempt may have started
 * the ad.
 *
 * <p>Budgets are in major units (10 = 10 EUR) in the ad account's currency. Ad
 * accounts are connected and picked in the OmniSocials dashboard. The scopes
 * {@code ads:read} and {@code ads:write} are opt-in on the API key (the "Ads"
 * switch); a key without them gets {@code 403 insufficient_scope}.
 */
public final class AdsResource extends ApiResource {

  private final AdDraftsResource drafts;

  public AdsResource(OmniSocials client) {
    super(client);
    this.drafts = new AdDraftsResource(client);
  }

  /** "Create an ad" drafts: one campaign per network, each with the same ads. */
  public AdDraftsResource drafts() {
    return drafts;
  }

  private static String campaign(long id) {
    return "/ads/campaigns/" + seg(String.valueOf(id));
  }

  /**
   * {@code GET /ads/accounts} - the connected ad accounts, plus
   * {@code platforms_available}. Accounts are connected and picked in the
   * dashboard; only {@code is_selected} accounts can run campaigns. Requires
   * {@code ads:read} (opt-in on the API key).
   */
  public JsonNode listAccounts() {
    return client.get("/ads/accounts");
  }

  /**
   * {@code GET /ads/capabilities} - what each network accepts for a new ad
   * (goals, media, text limits, buttons, minimum daily budget),
   * {@code create_networks} and {@code network_states} ({@code live},
   * {@code preview} or {@code coming_soon}). Requires {@code ads:read}.
   */
  public JsonNode capabilities() {
    return client.get("/ads/capabilities");
  }

  /** {@code GET /ads/campaigns} - list campaigns and boosts. Requires {@code ads:read}. */
  public JsonNode list() {
    return client.get("/ads/campaigns");
  }

  /**
   * {@code GET /ads/campaigns?status=&platform=&campaign_type=&limit=&offset=}
   * - list with filters. Query params: {@code status} ({@code draft} |
   * {@code pending_review} | {@code active} | {@code rejected} | {@code paused}
   * | {@code completed} | {@code error}), {@code platform}, {@code
   * campaign_type} ({@code campaign} | {@code boost}), {@code limit} (1 to 100,
   * default 25), {@code offset}. Requires {@code ads:read}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/ads/campaigns", query);
  }

  /** {@code GET /ads/campaigns/:id} - one campaign with its latest numbers. Requires {@code ads:read}. */
  public JsonNode get(long id) {
    return client.get(campaign(id));
  }

  /**
   * {@code POST /ads/boost} - boost a published post. Creates a DRAFT boost:
   * no spend until {@link #activate(long, Number)}. Params: {@code post_id},
   * {@code ad_account_id}, {@code budget} ({@code type}, {@code amount},
   * {@code currency}), optional {@code objective}, {@code targeting},
   * {@code schedule}, {@code creative}, {@code name}. TikTok needs
   * {@code creative.spark_auth_code}, Google needs {@code headlines},
   * {@code descriptions} and {@code logo_url}. Requires {@code ads:write}.
   */
  public JsonNode boost(Map<String, Object> params) {
    return client.post("/ads/boost", params);
  }

  /**
   * {@code POST /ads/boost-multi} - one DRAFT boost per ad account in
   * {@code ad_account_ids} (up to 10), each with the same budget, so the total
   * is amount x accounts. Answers {@code { results }}; one failed account never
   * stops the others. Requires {@code ads:write}.
   */
  public JsonNode boostMulti(Map<String, Object> params) {
    return client.post("/ads/boost-multi", params);
  }

  /**
   * {@code POST /ads/campaigns} - a DRAFT campaign without ads ({@code
   * ad_account_id}, {@code name}, {@code budget}, optional {@code objective},
   * {@code targeting}, {@code schedule}). It cannot start
   * ({@code campaign_has_no_ads}): use {@link AdDraftsResource#create(Map)}
   * for a new ad. Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/campaigns", params);
  }

  /**
   * {@code PATCH /ads/campaigns/:id} - change the {@code name}, {@code budget},
   * {@code targeting} or {@code schedule}. A budget raise can START REAL AD
   * SPEND: to raise the budget of a running campaign, show the person the new
   * amount, get their yes first, and pass it again as {@code
   * confirm_budget_amount}. Not retried after a 5xx or a timeout: read the
   * campaign with {@link #get(long)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode update(long id, Map<String, Object> params) {
    return client.patch(campaign(id), params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/:id/activate} - START REAL AD SPEND. Show the
   * person the budget (amount, currency, daily or lifetime) and get their yes
   * first; {@code confirmBudgetAmount} must equal the campaign's budget amount.
   * A campaign of a draft group answers {@code 409 use_draft_launch}: use
   * {@link AdDraftsResource#launch(String, Number)}. Not retried after a 5xx
   * or a timeout: read the campaign with {@link #get(long)} to see what
   * happened. Requires {@code ads:write}.
   */
  public JsonNode activate(long id, Number confirmBudgetAmount) {
    return client.post(
        campaign(id) + "/activate",
        Params.of("confirm_budget_amount", confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/activate-multi} - START REAL AD SPEND on up to
   * 10 campaigns. Every campaign's budget must equal {@code
   * confirmBudgetAmount}; show the person the total (amount x campaigns) and
   * get their yes first. Answers {@code { results }}, one per campaign. Not
   * retried after a 5xx or a timeout: read the campaigns with
   * {@link #get(long)} to see what happened. Requires {@code ads:write}.
   */
  public JsonNode activateMulti(List<Long> campaignIds, Number confirmBudgetAmount) {
    return client.post(
        "/ads/campaigns/activate-multi",
        Params.of("campaign_ids", campaignIds, "confirm_budget_amount", confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/:id/pause} - pause a campaign (also while in
   * review). Requires {@code ads:write}.
   */
  public JsonNode pause(long id) {
    return client.post(campaign(id) + "/pause");
  }

  /**
   * {@code POST /ads/campaigns/:id/resume} - resume a paused campaign. It
   * STARTS REAL AD SPEND again: show the person the budget and get their yes
   * first. Not retried after a 5xx or a timeout: read the campaign with
   * {@link #get(long)} to see what happened. Requires {@code ads:write}.
   */
  public JsonNode resume(long id) {
    return client.post(campaign(id) + "/resume", RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code DELETE /ads/campaigns/:id} - delete a campaign on the network too.
   * A campaign that spent is archived: its spend stays in the totals.
   * Requires {@code ads:write}.
   */
  public JsonNode delete(long id) {
    return client.delete(campaign(id));
  }

  /**
   * {@code GET /ads/overview} - connected accounts, campaigns per status and
   * total spend. Requires {@code ads:read}.
   */
  public JsonNode overview() {
    return client.get("/ads/overview");
  }

  /**
   * {@code GET /ads/suggestions} - organic posts that do better than the
   * workspace median: boost candidates. Read-only. Requires {@code ads:read}.
   */
  public JsonNode suggestions() {
    return client.get("/ads/suggestions");
  }

  /**
   * {@code POST /ads/research/keywords} - keyword ideas from seed
   * {@code keywords} (up to 20) or a {@code url}, through the connected Google
   * Ads account. Optional {@code country} (default {@code US}),
   * {@code language} (default {@code en}), {@code limit} (1 to 500) and
   * {@code ad_account_id}. Read-only and free. Requires {@code ads:read}.
   */
  public JsonNode researchKeywords(Map<String, Object> params) {
    return client.post("/ads/research/keywords", params);
  }

  /**
   * {@code POST /ads/research/keywords/metrics} - search numbers for an exact
   * {@code keywords} list. Read-only. Requires {@code ads:read}.
   */
  public JsonNode keywordMetrics(Map<String, Object> params) {
    return client.post("/ads/research/keywords/metrics", params);
  }
}
