package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/**
 * Partnership ads with creators on Meta. Accessed via {@code
 * client.ads().partnership()}. Run a creator's post through {@code
 * network.meta.partnership} on an ad.
 */
public final class AdPartnershipResource extends ApiResource {

  public AdPartnershipResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /ads/partnership/content?ad_account_id=} - creator posts you may
   * run as partnership ads. Requires {@code ads:read}.
   */
  public JsonNode listContent(long adAccountId) {
    return client.get("/ads/partnership/content", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code GET /ads/partnership/permissions?ad_account_id=} - the partnership
   * ad permissions. Requires {@code ads:read}.
   */
  public JsonNode listPermissions(long adAccountId) {
    return client.get("/ads/partnership/permissions", Params.of("ad_account_id", adAccountId));
  }

  /**
   * {@code POST /ads/partnership/permissions} - ask for or set a partnership ad
   * permission ({@code ad_account_id} and Meta's permission fields). Requires
   * {@code ads:write}.
   */
  public JsonNode setPermission(Map<String, Object> params) {
    return client.post("/ads/partnership/permissions", params);
  }
}
