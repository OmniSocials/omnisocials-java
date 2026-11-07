package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * The leads of the workspace. Accessed via {@code client.ads().leads()}. New
 * leads are copied from the networks every 15 minutes (webhook {@code
 * lead.received} per new lead). The answers are personal data; OmniSocials
 * stores them encrypted. Lead forms are in {@code client.ads().leadForms()}.
 */
public final class AdLeadsResource extends ApiResource {

  public AdLeadsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /ads/leads} - the leads of the workspace, newest first. Requires {@code ads:read}. */
  public JsonNode list() {
    return list(null);
  }

  /**
   * {@code GET /ads/leads} - by {@code form_id}, {@code status} ({@code new},
   * {@code contacted}, {@code qualified}, {@code won}, {@code lost}, {@code
   * spam}), {@code since}, {@code until}; {@code limit} up to 200 and {@code
   * cursor}. Requires {@code ads:read}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/ads/leads", query);
  }

  /**
   * {@code GET /ads/leads/counts} - lead counts by status, optionally of one
   * {@code form_id} (query may be null). Requires {@code ads:read}.
   */
  public JsonNode counts(Map<String, Object> query) {
    return client.get("/ads/leads/counts", query);
  }

  /** {@code PATCH /ads/leads/:leadId} - set a lead's {@code status} or {@code note}. Requires {@code ads:write}. */
  public JsonNode update(long leadId, Map<String, Object> params) {
    return client.patch("/ads/leads/" + seg(String.valueOf(leadId)), params);
  }
}
