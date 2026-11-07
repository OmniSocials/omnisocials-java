package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import java.util.Map;

/**
 * Comments on the ads of an OmniSocials campaign. Accessed via {@code
 * client.ads().comments()}. {@code commentId} is the network's comment id.
 */
public final class AdCommentsResource extends ApiResource {

  public AdCommentsResource(OmniSocials client) {
    super(client);
  }

  private static String comments(long campaignId, long adId) {
    return "/ads/campaigns/" + seg(String.valueOf(campaignId)) + "/ads/" + seg(String.valueOf(adId))
        + "/comments";
  }

  /**
   * {@code GET /ads/campaigns/:id/ads/:adId/comments} - comments on an ad
   * (query {@code cursor}; may be null). Requires {@code ads:read}.
   */
  public JsonNode list(long campaignId, long adId, Map<String, Object> query) {
    return client.get(comments(campaignId, adId), query);
  }

  /**
   * {@code POST /ads/campaigns/:id/ads/:adId/comments/:commentId/reply} -
   * answer a comment. Requires {@code ads:write}.
   */
  public JsonNode reply(long campaignId, long adId, String commentId, String text) {
    return client.post(comments(campaignId, adId) + "/" + seg(commentId) + "/reply", Params.of("text", text));
  }

  /**
   * {@code POST /ads/campaigns/:id/ads/:adId/comments/:commentId/hide} - hide a
   * comment ({@code hidden} true) or show it again (false). Requires {@code
   * ads:write}.
   */
  public JsonNode hide(long campaignId, long adId, String commentId, boolean hidden) {
    return client.post(comments(campaignId, adId) + "/" + seg(commentId) + "/hide", Params.of("hidden", hidden));
  }

  /**
   * {@code DELETE /ads/campaigns/:id/ads/:adId/comments/:commentId} - delete a
   * comment. Requires {@code ads:write}.
   */
  public JsonNode delete(long campaignId, long adId, String commentId) {
    return client.delete(comments(campaignId, adId) + "/" + seg(commentId));
  }
}
