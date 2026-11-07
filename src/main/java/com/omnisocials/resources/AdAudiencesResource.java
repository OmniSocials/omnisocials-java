package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/**
 * Custom audiences of an ad account: customer lists, lookalikes and company
 * lists. Accessed via {@code client.ads().audiences()}. {@code audienceId} is
 * the network's own id; use an audience in {@code
 * targeting.networks.<network>.audiences.include}.
 */
public final class AdAudiencesResource extends ApiResource {

  public AdAudiencesResource(OmniSocials client) {
    super(client);
  }

  private static String audience(String audienceId) {
    return "/ads/audiences/" + seg(audienceId);
  }

  /**
   * {@code GET /ads/audiences} - the custom audiences of {@code ad_account_id}
   * (query; {@code cursor} for the next page). Requires {@code ads:read}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/ads/audiences", query);
  }

  /**
   * {@code POST /ads/audiences} - create a custom audience: {@code
   * ad_account_id}, {@code name}, optional {@code description}, {@code kind}
   * ({@code customer_list}, {@code lookalike}, {@code company_list}), {@code
   * lookalike} ({@code source_audience_id}, {@code country}, {@code ratio} 0.01
   * to 0.2), {@code customer_file_source} (Meta customer lists: {@code
   * user_provided}, {@code partner_provided} or {@code both}, as the person
   * says) and the first members of a customer list as {@code schema} + {@code
   * rows} (the shape of {@link #addMembers(String, Map)}; Pinterest makes a
   * list only together with its first people). Requires {@code ads:write}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/ads/audiences", params);
  }

  /** {@code GET /ads/audiences/:audienceId?ad_account_id=} - one audience. Requires {@code ads:read}. */
  public JsonNode get(String audienceId, long adAccountId) {
    return client.get(audience(audienceId), Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code PATCH /ads/audiences/:audienceId} - rename an audience ({@code
   * ad_account_id}, {@code name}, {@code description}). Requires {@code
   * ads:write}.
   */
  public JsonNode update(String audienceId, Map<String, Object> params) {
    return client.patch(audience(audienceId), params);
  }

  /** {@code DELETE /ads/audiences/:audienceId?ad_account_id=} - delete an audience. Requires {@code ads:write}. */
  public JsonNode delete(String audienceId, long adAccountId) {
    return client.delete(audience(audienceId), Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/audiences/:audienceId/members} - add people to a customer
   * list: {@code ad_account_id}, {@code schema} (column names: {@code email},
   * {@code phone} with the country code, {@code first_name}, {@code
   * last_name}, {@code city}, {@code state}, {@code zip}, {@code country},
   * {@code external_id}, {@code dob_year}, {@code gender}, {@code maid}) and
   * {@code rows} (at most 10,000). Send the raw values: OmniSocials normalizes
   * and SHA-256 hashes them before they leave and stores nothing. Requires
   * {@code ads:write}.
   */
  public JsonNode addMembers(String audienceId, Map<String, Object> params) {
    return client.post(audience(audienceId) + "/members", params);
  }

  /**
   * {@code DELETE /ads/audiences/:audienceId/members} - remove people from a
   * customer list (the same {@code ad_account_id}, {@code schema} and {@code
   * rows}, sent as the JSON body). Requires {@code ads:write}.
   */
  public JsonNode removeMembers(String audienceId, Map<String, Object> params) {
    return client.delete(audience(audienceId) + "/members", null, params);
  }

  /**
   * {@code POST /ads/audiences/:audienceId/companies} - add companies to a
   * company list (LinkedIn): {@code ad_account_id} and {@code companies} (each
   * {@code name}, {@code domain} or {@code linkedin_url}). Requires {@code
   * ads:write}.
   */
  public JsonNode addCompanies(String audienceId, Map<String, Object> params) {
    return client.post(audience(audienceId) + "/companies", params);
  }
}
