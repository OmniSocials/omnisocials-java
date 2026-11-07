package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.RetryPolicy;
import java.util.Map;

/**
 * One connected ad account: health, money, settings, people, the change log,
 * businesses and identities. Accessed via {@code client.ads().accounts()}.
 * List the accounts with {@link AdsResource#listAccounts()}; {@code
 * accountId} is the OmniSocials ad account id.
 */
public final class AdAccountsResource extends ApiResource {

  public AdAccountsResource(OmniSocials client) {
    super(client);
  }

  private static String account(long accountId) {
    return "/ads/accounts/" + seg(String.valueOf(accountId));
  }

  /**
   * {@code GET /ads/accounts/:accountId/health} - check the account before an
   * ad starts (status, payment, spend limit, Page, pixel); failed checks carry
   * a {@code fix_url}. Requires {@code ads:read}.
   */
  public JsonNode health(long accountId) {
    return client.get(account(accountId) + "/health");
  }

  /**
   * {@code GET /ads/accounts/:accountId/finance} - spend limit, amount spent,
   * balance and payment method. Requires {@code ads:read}.
   */
  public JsonNode finance(long accountId) {
    return client.get(account(accountId) + "/finance");
  }

  /**
   * {@code PATCH /ads/accounts/:accountId/settings} - change {@code spend_cap},
   * {@code dsa_beneficiary}, {@code dsa_payor} or {@code name}. A HIGHER SPEND
   * CAP lets the account spend more: show the person the new cap and get their
   * yes first, then pass it as {@code confirm_budget_amount}; removing the cap
   * ({@code spend_cap} null) needs {@code confirm_budget_amount}
   * {@code "remove"}. A spend cap change needs a key with the opt-in
   * {@code ads:spend} scope ("Start ads"); without it, and on Google
   * ({@code api_draft_only}) always, the call answers {@code 403 start_in_app}
   * with the page where a person does it in the app. The name and the DSA names
   * stay open. Not retried after a 5xx or a timeout: read
   * {@link #finance(long)} to see what happened. Requires {@code ads:write}.
   */
  public JsonNode updateSettings(long accountId, Map<String, Object> params) {
    return client.patch(account(accountId) + "/settings", params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /** {@code GET /ads/accounts/:accountId/users} - people with access. Requires {@code ads:read}. */
  public JsonNode listUsers(long accountId) {
    return client.get(account(accountId) + "/users");
  }

  /**
   * {@code POST /ads/accounts/:accountId/users} - give a person access
   * (LinkedIn): {@code role} and {@code email} or {@code member_id}. Requires
   * {@code ads:write}.
   */
  public JsonNode addUser(long accountId, Map<String, Object> params) {
    return client.post(account(accountId) + "/users", params);
  }

  /** {@code DELETE /ads/accounts/:accountId/users/:userId} - remove a person's access. Requires {@code ads:write}. */
  public JsonNode removeUser(long accountId, String userId) {
    return client.delete(account(accountId) + "/users/" + seg(userId));
  }

  /**
   * {@code GET /ads/accounts/:accountId/activity} - the change log (query
   * {@code since}, {@code until}, {@code cursor}; may be null). Requires
   * {@code ads:read}.
   */
  public JsonNode activity(long accountId, Map<String, Object> query) {
    return client.get(account(accountId) + "/activity", query);
  }

  /**
   * {@code GET /ads/accounts/:accountId/businesses} - business portfolios
   * (Meta) and Business Centers (TikTok). Requires {@code ads:read}.
   */
  public JsonNode businesses(long accountId) {
    return client.get(account(accountId) + "/businesses");
  }

  /** {@code GET /ads/accounts/:accountId/dsa} - the EU DSA payer and beneficiary (Meta). Requires {@code ads:read}. */
  public JsonNode dsa(long accountId) {
    return client.get(account(accountId) + "/dsa");
  }

  /** {@code GET /ads/accounts/:accountId/hierarchy} - the manager account hierarchy (Google). Requires {@code ads:read}. */
  public JsonNode hierarchy(long accountId) {
    return client.get(account(accountId) + "/hierarchy");
  }

  /**
   * {@code GET /ads/accounts/:accountId/identities} - Pages, profiles and
   * identities an ad of this account can run as. Requires {@code ads:read}.
   */
  public JsonNode identities(long accountId) {
    return client.get(account(accountId) + "/identities");
  }
}
