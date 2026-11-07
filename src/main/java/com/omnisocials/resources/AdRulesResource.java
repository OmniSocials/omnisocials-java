package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.omnisocials.RetryPolicy;
import java.util.Map;

/**
 * Automated rules. Accessed via {@code client.ads().rules()}. A rule checks
 * campaigns on a schedule and acts on each one that meets every condition:
 * {@code notify} (webhook {@code ad_rule.triggered}), {@code pause}, {@code
 * activate} or {@code adjust_budget}.
 *
 * <p>A rule that notifies, pauses or lowers a budget works with
 * {@code ads:write}. A rule that can raise spend ({@code activate}, a positive
 * {@code adjust_budget}) needs a key with the opt-in {@code ads:spend} scope
 * (else {@code 403 rule_raises_spend}) and {@code action.max_daily_budget},
 * confirmed by the person as {@code confirm_max_daily_budget}. Never invent or
 * confirm that amount yourself. At run time a rule never acts on Google.
 * {@link #create(Map)}, {@link #update(long, Map)} and
 * {@link #undo(long, Number)} are retried on a 429 only, never after a 5xx or a
 * timeout.
 */
public final class AdRulesResource extends ApiResource {

  public AdRulesResource(OmniSocials client) {
    super(client);
  }

  private static String rule(long ruleId) {
    return "/ads/rules/" + seg(String.valueOf(ruleId));
  }

  /** {@code GET /ads/rules} - the automated rules of the workspace. Requires {@code ads:read}. */
  public JsonNode list() {
    return client.get("/ads/rules");
  }

  /**
   * {@code POST /ads/rules} - create a rule: {@code name}, {@code conditions},
   * {@code action} and optionally {@code ad_account_id}, {@code enabled} (off
   * until true), {@code scope}, {@code every_minutes},
   * {@code cooldown_minutes}, {@code max_actions_per_run}. A rule that can
   * RAISE AD SPEND needs {@code confirm_max_daily_budget} =
   * {@code action.max_daily_budget}, after the person agreed to it, and a key
   * with {@code ads:spend} (else {@code 403 rule_raises_spend}). Preview it
   * first with {@link #preview(Map)}. Not retried after a 5xx or a timeout:
   * list the rules to see what happened. Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/rules", params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /** {@code GET /ads/rules/templates} - ready-made rules. Requires {@code ads:read}. */
  public JsonNode templates() {
    return client.get("/ads/rules/templates");
  }

  /**
   * {@code POST /ads/rules/preview} - what an unsaved rule (the body of
   * {@link #create(Map)}) would match and do now. Changes nothing. Requires
   * {@code ads:read}.
   */
  public JsonNode preview(Map<String, Object> params) {
    return client.post("/ads/rules/preview", params);
  }

  /**
   * {@code GET /ads/rules/activity} - every change the rules made, with before
   * and after (query {@code rule_id}, {@code limit}; may be null). Requires
   * {@code ads:read}.
   */
  public JsonNode activity(Map<String, Object> query) {
    return client.get("/ads/rules/activity", query);
  }

  /**
   * {@code POST /ads/rules/activity/:activityId/undo} - undo a change a rule
   * made. Undoing a pause switches the campaign on again and undoing a budget
   * cut raises the budget back: both START OR RAISE REAL AD SPEND and need
   * {@code confirmBudgetAmount} = the budget, after the person agreed to it,
   * and a key with {@code ads:spend} (else, and on Google always,
   * {@code 403 start_in_app}). Undoing a budget raise needs no amount (pass
   * {@code null}). Not retried after a 5xx or a timeout. Requires
   * {@code ads:write}.
   */
  public JsonNode undo(long activityId, Number confirmBudgetAmount) {
    String path = "/ads/rules/activity/" + seg(String.valueOf(activityId)) + "/undo";
    return confirmBudgetAmount == null
        ? client.post(path, RetryPolicy.RATE_LIMIT_ONLY)
        : client.post(
            path, Params.of("confirm_budget_amount", confirmBudgetAmount), RetryPolicy.RATE_LIMIT_ONLY);
  }

  /** {@code GET /ads/rules/:ruleId} - one rule. Requires {@code ads:read}. */
  public JsonNode get(long ruleId) {
    return client.get(rule(ruleId));
  }

  /**
   * {@code PATCH /ads/rules/:ruleId} - change a rule, also {@code enabled}. A
   * changed action that can RAISE AD SPEND needs
   * {@code confirm_max_daily_budget} again, after the person agreed to it, and
   * a key with {@code ads:spend} (else {@code 403 rule_raises_spend}). Not
   * retried after a 5xx or a timeout: read the rule with {@link #get(long)}.
   * Requires {@code ads:write}.
   */
  public JsonNode update(long ruleId, Map<String, Object> params) {
    return client.patch(rule(ruleId), params, RetryPolicy.RATE_LIMIT_ONLY);
  }

  /** {@code DELETE /ads/rules/:ruleId} - delete a rule. Requires {@code ads:write}. */
  public JsonNode delete(long ruleId) {
    return client.delete(rule(ruleId));
  }

  /**
   * {@code POST /ads/rules/:ruleId/preview} - what a saved rule would match
   * and do now. Changes nothing. Requires {@code ads:read}.
   */
  public JsonNode previewSaved(long ruleId) {
    return client.post(rule(ruleId) + "/preview");
  }

  /** {@code GET /ads/rules/:ruleId/runs} - the runs of a rule (query may be null). Requires {@code ads:read}. */
  public JsonNode runs(long ruleId, Map<String, Object> query) {
    return client.get(rule(ruleId) + "/runs", query);
  }
}
