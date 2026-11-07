package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The campaigns, ad sets and ads of an ad account as the network has them,
 * also the ones made outside OmniSocials, and the import of those campaigns.
 * Accessed via {@code client.ads().objects()}.
 *
 * <p>{@code level} is {@code campaign}, {@code ad_set} or {@code ad};
 * {@code externalId} is the network's own id. Objects of OmniSocials
 * campaigns answer {@code 409 use_campaign_routes}: change those through
 * {@code client.ads()}. A network without the feature answers {@code 400
 * not_supported_on_network} ({@link AdsResource#capabilities()} lists
 * {@code features}).
 */
public final class AdObjectsResource extends ApiResource {

  public AdObjectsResource(OmniSocials client) {
    super(client);
  }

  private static String account(long accountId) {
    return "/ads/accounts/" + seg(String.valueOf(accountId));
  }

  private static String object(long accountId, String level, String externalId) {
    return account(accountId) + "/objects/" + seg(level) + "/" + seg(externalId);
  }

  /** {@code GET /ads/accounts/:accountId/tree} with the last 30 days. Requires {@code ads:read}. */
  public JsonNode tree(long accountId) {
    return tree(accountId, null);
  }

  /**
   * {@code GET /ads/accounts/:accountId/tree} - campaigns, ad sets and ads of
   * the ad account, read live from the network with the numbers of the
   * period. Query: {@code from}, {@code to} (at most 90 days), {@code status}
   * (a list or a comma list), {@code limit} (up to 100), {@code cursor}. Each
   * campaign says {@code source} ({@code omnisocials} or {@code external}).
   * Requires {@code ads:read}.
   */
  public JsonNode tree(long accountId, Map<String, Object> query) {
    return client.get(account(accountId) + "/tree", query);
  }

  /**
   * {@code GET /ads/accounts/:accountId/objects/:level/:externalId} - live
   * details of one campaign, ad set or ad. Requires {@code ads:read}.
   */
  public JsonNode get(long accountId, String level, String externalId) {
    return client.get(object(accountId, level, externalId));
  }

  /**
   * {@code PATCH /ads/accounts/:accountId/objects/:level/:externalId} - change
   * the {@code name}, {@code budget} or {@code end_at} of a campaign or ad set
   * made outside OmniSocials. A higher budget RAISES REAL AD SPEND: show the
   * person the new amount and get their yes first, then pass it again as
   * {@code confirm_budget_amount}. It changes the campaign on the network, so
   * it needs a key with the opt-in {@code ads:spend} scope ("Start ads");
   * without it, and on Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app.
   * Not retried after a 5xx or a timeout: read the object with
   * {@link #get(long, String, String)} to see what happened. Requires
   * {@code ads:write}.
   */
  public JsonNode update(long accountId, String level, String externalId, Map<String, Object> params) {
    return client.patch(object(accountId, level, externalId), params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code PUT /ads/accounts/:accountId/objects/:level/:externalId/status} -
   * pause ({@code paused}) or switch on ({@code active}) one campaign, ad set
   * or ad. Switching on STARTS REAL AD SPEND: show the person the budget that
   * pays (the object's own or its parent's) and get their yes first, then pass
   * it as {@code confirmBudgetAmount}. Switching on needs a key with the opt-in
   * {@code ads:spend} scope ("Start ads"); without it, and on Google
   * ({@code api_draft_only}) always, the call answers {@code 403 start_in_app}
   * with the page where a person does it in the app. Pausing needs no amount
   * (pass {@code null}) and stays open (Google: {@code start_in_app}). Not
   * retried after a 5xx or a timeout: read the object to see what happened.
   * Requires {@code ads:write}.
   */
  public JsonNode setStatus(
      long accountId, String level, String externalId, String status, Number confirmBudgetAmount) {
    return client.put(
        object(accountId, level, externalId) + "/status",
        statusBody(status, confirmBudgetAmount),
        RetryPolicy.RATE_LIMIT_ONLY);
  }

  /**
   * {@code POST /ads/accounts/:accountId/objects/:level/:externalId/duplicate}
   * - copy a campaign, ad set or ad made outside OmniSocials; the copy is
   * paused. Params: {@code deep} (default true), {@code rename}. Meta only for
   * now. Needs a key with the opt-in {@code ads:spend} scope ("Start ads");
   * without it, and on Google ({@code api_draft_only}) always, the call answers
   * {@code 403 start_in_app} with the page where a person does it in the app.
   * Copy an OmniSocials campaign as a draft with
   * {@link AdsResource#duplicate(long, Map)}. Requires {@code ads:write}.
   */
  public JsonNode duplicate(long accountId, String level, String externalId, Map<String, Object> params) {
    return client.post(object(accountId, level, externalId) + "/duplicate", params);
  }

  /**
   * {@code GET /ads/accounts/:accountId/import} - the import state of the
   * campaigns made outside OmniSocials: {@code status} ({@code not_started},
   * {@code pending}, {@code done}, {@code failed}) and {@code
   * campaigns_imported}. Requires {@code ads:read}.
   */
  public JsonNode importStatus(long accountId) {
    return client.get(account(accountId) + "/import");
  }

  /**
   * {@code POST /ads/accounts/:accountId/import} - read the ad account again at
   * the next sync (within 15 minutes). The first import reads the last 90
   * days; webhook {@code ads.initial_sync_completed} when it is done. Answers
   * 202. Requires {@code ads:write}.
   */
  public JsonNode startImport(long accountId) {
    return client.post(account(accountId) + "/import");
  }

  /** {@code GET /ads/external-campaigns} - imported campaigns. Requires {@code ads:read}. */
  public JsonNode listExternal() {
    return listExternal(null);
  }

  /**
   * {@code GET /ads/external-campaigns} - imported campaigns, by {@code
   * ad_account_id}, {@code platform} or {@code status}; {@code limit} up to 200
   * and {@code offset}. Requires {@code ads:read}.
   */
  public JsonNode listExternal(Map<String, Object> query) {
    return client.get("/ads/external-campaigns", query);
  }

  /**
   * {@code GET /ads/external-campaigns/:id} - one imported campaign with its
   * daily numbers (query {@code from}, {@code to}; may be null). Requires
   * {@code ads:read}.
   */
  public JsonNode getExternal(long id, Map<String, Object> query) {
    return client.get("/ads/external-campaigns/" + seg(String.valueOf(id)), query);
  }

  static Map<String, Object> statusBody(String status, Number confirmBudgetAmount) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", status);
    if (confirmBudgetAmount != null) {
      body.put("confirm_budget_amount", confirmBudgetAmount);
    }
    return body;
  }
}
