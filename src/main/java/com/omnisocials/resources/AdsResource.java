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
 * <p>Ads spend real money once a campaign starts. Boosts and drafts are created
 * as drafts (no spend). Starting or raising spend needs a key with the opt-in
 * {@code ads:spend} scope ("Start ads", a second switch under "Ads"; its
 * creator needs {@code manage_ads}): {@link #activate(long, Number)},
 * {@link #activateMulti(List, Number)}, {@link #resume(long)},
 * {@link AdDraftsResource#launch(String, Number)}, a budget raise or any change
 * of a campaign that reached the network through {@link #update(long, Map)},
 * and the parity calls that switch something on or raise a budget:
 * {@link #bulkStatus(List, String, Number)} (resuming),
 * {@code campaignAds().setStatus} (switching on), {@code campaignAds().add} on
 * a running campaign, {@code objects().update}, {@code objects().setStatus}
 * (switching on), {@code objects().duplicate}, {@code rules().create},
 * {@code rules().update} and {@code rules().undo} (a rule or undo that switches
 * on or raises), {@code accounts().updateSettings} (a spend cap change),
 * {@code reachFrequency().reserve}, {@code studies().create} and
 * {@code budgetSchedules().create}. Each repeats the amount. Without
 * {@code ads:spend} these calls answer {@code 403 start_in_app} with the page
 * in the app where a person does it (rules: {@code 403 rule_raises_spend}); on
 * Google ({@code api_draft_only} on {@link #capabilities()}) they always do,
 * also with {@code ads:spend}. {@link #pause(long)} and {@link #delete(long)}
 * stay open, except on the networks in {@code api_stop_in_app} (Google).
 *
 * <p>Take the amount from the person's explicit yes; never invent or confirm
 * it yourself. The calls that start or raise spend are retried on a 429 only,
 * never after a 5xx or a timeout: the first attempt may have started the ad.
 * Pausing never needs an amount.
 *
 * <p>Budgets are in major units (10 = 10 EUR) in the ad account's currency. An
 * ads error may carry {@code param} (the field it is about) in
 * {@code ApiException.getBody()}. Ad accounts are connected and picked in the
 * OmniSocials dashboard. The scopes {@code ads:read} and {@code ads:write} are
 * opt-in on the API key (the "Ads" switch); a key without them gets
 * {@code 403 insufficient_scope}, and every write also needs {@code manage_ads}
 * for the person behind the key. A network without a feature answers
 * {@code 400 not_supported_on_network}; {@link #capabilities()} lists
 * {@code features} per network.
 */
public final class AdsResource extends ApiResource {

  private final AdDraftsResource drafts;
  private final AdObjectsResource objects;
  private final AdTargetingResource targeting;
  private final AdAudiencesResource audiences;
  private final AdCampaignAdsResource campaignAds;
  private final AdLeadFormsResource leadForms;
  private final AdLeadsResource leads;
  private final AdConversionsResource conversions;
  private final AdInsightsResource insights;
  private final AdReportsResource reports;
  private final AdRulesResource rules;
  private final AdResultsResource results;
  private final AdMediaResource media;
  private final AdPreviewsResource previews;
  private final AdPartnershipResource partnership;
  private final AdCommentsResource comments;
  private final AdAccountsResource accounts;
  private final AdReachFrequencyResource reachFrequency;
  private final AdStudiesResource studies;
  private final AdBudgetSchedulesResource budgetSchedules;
  private final AdGoogleResource google;

  public AdsResource(OmniSocials client) {
    super(client);
    this.drafts = new AdDraftsResource(client);
    this.objects = new AdObjectsResource(client);
    this.targeting = new AdTargetingResource(client);
    this.audiences = new AdAudiencesResource(client);
    this.campaignAds = new AdCampaignAdsResource(client);
    this.leadForms = new AdLeadFormsResource(client);
    this.leads = new AdLeadsResource(client);
    this.conversions = new AdConversionsResource(client);
    this.insights = new AdInsightsResource(client);
    this.reports = new AdReportsResource(client);
    this.rules = new AdRulesResource(client);
    this.results = new AdResultsResource(client);
    this.media = new AdMediaResource(client);
    this.previews = new AdPreviewsResource(client);
    this.partnership = new AdPartnershipResource(client);
    this.comments = new AdCommentsResource(client);
    this.accounts = new AdAccountsResource(client);
    this.reachFrequency = new AdReachFrequencyResource(client);
    this.studies = new AdStudiesResource(client);
    this.budgetSchedules = new AdBudgetSchedulesResource(client);
    this.google = new AdGoogleResource(client);
  }

  /** "Create an ad" drafts: one campaign per network, each with the same ads. */
  public AdDraftsResource drafts() {
    return drafts;
  }

  /** Campaigns, ad sets and ads as the network has them, and the import of campaigns made outside OmniSocials. */
  public AdObjectsResource objects() {
    return objects;
  }

  /** Targeting search, browse, reach estimate and bid suggestions. */
  public AdTargetingResource targeting() {
    return targeting;
  }

  /** Custom audiences: customer lists, lookalikes, company lists. */
  public AdAudiencesResource audiences() {
    return audiences;
  }

  /** The ads of an OmniSocials campaign: list, add, read, status, review. */
  public AdCampaignAdsResource campaignAds() {
    return campaignAds;
  }

  /** Lead forms. */
  public AdLeadFormsResource leadForms() {
    return leadForms;
  }

  /** The leads the lead forms bring. */
  public AdLeadsResource leads() {
    return leads;
  }

  /** Pixels, conversion goals, custom conversions and server events. */
  public AdConversionsResource conversions() {
    return conversions;
  }

  /** Live numbers, campaign and ad numbers, timeline, rankings and alerts. */
  public AdInsightsResource insights() {
    return insights;
  }

  /** Long reports that run in the background. */
  public AdReportsResource reports() {
    return reports;
  }

  /** Automated rules. */
  public AdRulesResource rules() {
    return rules;
  }

  /** Your own numbers per campaign and day. */
  public AdResultsResource results() {
    return results;
  }

  /** The network's own image and video library. */
  public AdMediaResource media() {
    return media;
  }

  /** Previews drawn by the network. */
  public AdPreviewsResource previews() {
    return previews;
  }

  /** Partnership ads with creators (Meta). */
  public AdPartnershipResource partnership() {
    return partnership;
  }

  /** Comments on ads. */
  public AdCommentsResource comments() {
    return comments;
  }

  /** One ad account: health, finance, settings, people, activity, identities. */
  public AdAccountsResource accounts() {
    return accounts;
  }

  /** Reach and frequency buying (Meta). */
  public AdReachFrequencyResource reachFrequency() {
    return reachFrequency;
  }

  /** A/B tests and lift studies (Meta). */
  public AdStudiesResource studies() {
    return studies;
  }

  /** Planned budget increases (Meta). */
  public AdBudgetSchedulesResource budgetSchedules() {
    return budgetSchedules;
  }

  /** Google Ads reports. */
  public AdGoogleResource google() {
    return google;
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
   * (goals, media, text limits, buttons, minimum daily budget, and per network
   * {@code bid_strategies}, {@code settings} and {@code ad_fields}),
   * {@code create_networks}, {@code network_states} ({@code live},
   * {@code preview} or {@code coming_soon}), {@code features} (per network, the
   * feature keys its API offers here) with {@code feature_labels},
   * {@code targeting_types} (per network, the types of
   * {@code targeting().search} and of {@code targeting.networks}),
   * {@code api_goals} ({@code lead_generation}, {@code conversions},
   * {@code messages}, {@code calls}: API only; each network's goal entry says
   * what it {@code needs}), {@code api_draft_only} (networks where every API
   * caller stops at the draft, also a key with {@code ads:spend}: Google) and
   * {@code api_stop_in_app} (networks where the API may not even pause or
   * delete a campaign that reached the network: Google). Requires
   * {@code ads:read}.
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
   * {@code POST /ads/boost} - boost a published post. Creates a DRAFT boost
   * that spends nothing; the person starts it in the app. Params:
   * {@code post_id}, {@code ad_account_id}, {@code budget} ({@code type},
   * {@code amount}, {@code currency}), optional {@code objective},
   * {@code targeting}, {@code schedule}, {@code creative}, {@code name}. TikTok
   * needs {@code creative.spark_auth_code}, Google needs {@code headlines},
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
   * {@code PATCH /ads/campaigns/:id} - change the {@code name},
   * {@code objective}, {@code budget}, {@code targeting} (also
   * {@code targeting.network}), {@code schedule}, or the {@code settings} of a
   * campaign that never started (after the start {@code 400 settings_locked}:
   * make a copy with {@link #duplicate(long, Map)}). A draft changes with
   * {@code ads:write}. A campaign that reached the network changes only with a
   * key that has the opt-in {@code ads:spend} scope ("Start ads"); without it,
   * and on Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app. A
   * budget raise can START REAL AD SPEND: to raise the budget of a running
   * campaign, show the person the new amount, get their yes first, and pass it
   * again as {@code confirm_budget_amount}. Not retried after a 5xx or a
   * timeout: read the campaign with {@link #get(long)} to see what happened.
   * Requires {@code ads:write}.
   */
  public JsonNode update(long id, Map<String, Object> params) {
    return client.patch(campaign(id), params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/:id/activate} - START REAL AD SPEND. Needs a key
   * with the opt-in {@code ads:spend} scope ("Start ads"); without it, and on
   * Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app.
   * Show the person the budget (amount, currency, daily or lifetime) and get
   * their yes first; {@code confirmBudgetAmount} must equal the campaign's
   * budget amount. A campaign of a draft group answers
   * {@code 409 use_draft_launch}: use
   * {@link AdDraftsResource#launch(String, Number)}. Not retried after a 5xx or
   * a timeout: read the campaign with {@link #get(long)} to see what happened.
   * Requires {@code ads:write}.
   */
  public JsonNode activate(long id, Number confirmBudgetAmount) {
    return client.post(
        campaign(id) + "/activate",
        Params.of("confirm_budget_amount", confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/activate-multi} - START REAL AD SPEND on up to
   * 10 campaigns. Needs a key with the opt-in {@code ads:spend} scope ("Start
   * ads"); without it, and on Google ({@code api_draft_only}) always, a
   * campaign comes back {@code ok: false} with code {@code start_in_app} and
   * the page where a person starts it in the app. Every campaign's budget must
   * equal {@code confirmBudgetAmount}; show the person the total (amount x
   * campaigns) and get their yes first. Answers {@code { results }}, one per
   * campaign. Not retried after a 5xx or a timeout: read the campaigns with
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
   * review). Open with {@code ads:write}, except on the networks in
   * {@code api_stop_in_app} (Google): there a campaign that reached the network
   * answers {@code 403 start_in_app}. Requires {@code ads:write}.
   */
  public JsonNode pause(long id) {
    return client.post(campaign(id) + "/pause");
  }

  /**
   * {@code POST /ads/campaigns/:id/resume} - resume a paused campaign. It
   * STARTS REAL AD SPEND again. Needs a key with the opt-in {@code ads:spend}
   * scope ("Start ads"); without it, and on Google ({@code api_draft_only})
   * always, the call answers {@code 403 start_in_app} with the page where a
   * person does it in the app. Only a paused campaign resumes
   * ({@code 409 not_paused}). Not retried after a 5xx or a timeout: read the
   * campaign with {@link #get(long)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode resume(long id) {
    return client.post(campaign(id) + "/resume", RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code DELETE /ads/campaigns/:id} - delete a campaign on the network too. A
   * campaign that spent is archived: its spend stays in the totals. Open with
   * {@code ads:write}, except on the networks in {@code api_stop_in_app}
   * (Google): there a campaign that reached the network answers
   * {@code 403 start_in_app}. Requires {@code ads:write}.
   */
  public JsonNode delete(long id) {
    return client.delete(campaign(id));
  }

  /**
   * {@code GET /ads/overview} - connected accounts, campaigns per status and
   * the totals. {@code totals.spend} is null when the campaigns use more than
   * one currency ({@code totals.currency} names the one currency); read
   * {@code totals_by_currency}, one row per currency with {@code currency},
   * {@code spend}, {@code impressions} and {@code clicks}. Requires
   * {@code ads:read}.
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

  /**
   * {@code POST /ads/campaigns/bulk-status} - pause ({@code paused}) or resume
   * ({@code active}) up to 50 OmniSocials campaigns. Resuming STARTS REAL AD
   * SPEND again: every campaign's budget must equal {@code confirmBudgetAmount}
   * (one amount for all, checked before any change); show the person the total
   * and get their yes first. Resuming needs a key with {@code ads:spend}:
   * without it, and on Google always, a result answers {@code start_in_app}.
   * Pausing needs no amount (pass {@code null}; Google: {@code start_in_app}).
   * Answers {@code results} per campaign. Not retried after a 5xx or a timeout:
   * read the campaigns with {@link #get(long)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode bulkStatus(List<Long> campaignIds, String status, Number confirmBudgetAmount) {
    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("campaign_ids", campaignIds);
    body.put("status", status);
    if (confirmBudgetAmount != null) {
      body.put("confirm_budget_amount", confirmBudgetAmount);
    }
    return client.post("/ads/campaigns/bulk-status", body, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/campaigns/:id/duplicate} - copy an OmniSocials campaign as
   * a new DRAFT (same ad account, goal, budget, targeting, settings and ads;
   * optional {@code name}, params may be null). A draft never spends. Requires
   * {@code ads:write}.
   */
  public JsonNode duplicate(long id, Map<String, Object> params) {
    return client.post(campaign(id) + "/duplicate", params);
  }
}
