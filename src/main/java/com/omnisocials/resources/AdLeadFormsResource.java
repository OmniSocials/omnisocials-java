package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/**
 * Lead forms of an ad account. Accessed via {@code client.ads().leadForms()}.
 * The leads they bring are in {@code client.ads().leads()}.
 */
public final class AdLeadFormsResource extends ApiResource {

  public AdLeadFormsResource(OmniSocials client) {
    super(client);
  }

  private static String form(long formId) {
    return "/ads/lead-forms/" + seg(String.valueOf(formId));
  }

  /**
   * {@code GET /ads/lead-forms?ad_account_id=} - the lead forms of an ad
   * account. {@code id} is the OmniSocials form id, {@code external_id} the
   * network's (use that as {@code settings.lead_form_id}). Requires {@code
   * ads:read}.
   */
  public JsonNode list(long adAccountId) {
    return client.get("/ads/lead-forms", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/lead-forms} - create a lead form: {@code ad_account_id},
   * {@code name}, {@code privacy_policy_url}, {@code questions}, {@code
   * thank_you}, optional {@code locale}, {@code headline}, {@code description}
   * and {@code accept_terms} (the person accepts the network's lead ad terms;
   * Pinterest refuses a form without it; only send true after the person said
   * yes). Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/lead-forms", params);
  }

  /** {@code GET /ads/lead-forms/:formId} - one lead form. Requires {@code ads:read}. */
  public JsonNode get(long formId) {
    return client.get(form(formId));
  }

  /**
   * {@code PATCH /ads/lead-forms/:formId} - archive or open a form ({@code
   * status} {@code active} or {@code archived}) or switch its lead sync
   * ({@code sync_enabled}). Requires {@code ads:write}.
   */
  public JsonNode update(long formId, Map<String, Object> params) {
    return client.patch(form(formId), params);
  }

  /**
   * {@code DELETE /ads/lead-forms/:formId} - archive a lead form; the leads
   * already copied stay. Requires {@code ads:write}.
   */
  public JsonNode delete(long formId) {
    return client.delete(form(formId));
  }

  /**
   * {@code POST /ads/lead-forms/:formId/test-leads} - send a test lead
   * (optional {@code fields}; may be null). The next sync copies it, marked
   * {@code is_test}. Requires {@code ads:write}.
   */
  public JsonNode sendTestLead(long formId, Map<String, Object> params) {
    return client.post(form(formId) + "/test-leads", params);
  }
}
