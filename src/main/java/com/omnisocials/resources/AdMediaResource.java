package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * The network's own image and video library of an ad account. Accessed via
 * {@code client.ads().media()}. Use an uploaded file in an ad as {@code
 * network.<network>.media_ref}.
 */
public final class AdMediaResource extends ApiResource {

  public AdMediaResource(OmniSocials client) {
    super(client);
  }

  private static String media(String mediaId) {
    return "/ads/media/" + seg(mediaId);
  }

  /**
   * {@code GET /ads/media} - the library (query {@code ad_account_id},
   * optional {@code type} {@code image} or {@code video}, {@code cursor}).
   * Requires {@code ads:read}.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/ads/media", query);
  }

  /**
   * {@code POST /ads/media} - upload an image or video to the network ahead of
   * an ad: {@code ad_account_id}, {@code type}, {@code url}, optional {@code
   * name}. Requires {@code ads:write}.
   */
  public JsonNode upload(Map<String, Object> params) {
    return client.post("/ads/media", params);
  }

  /**
   * {@code GET /ads/media/:mediaId} - the processing status of an uploaded
   * file (query {@code ad_account_id}, optional {@code type}). Requires {@code
   * ads:read}.
   */
  public JsonNode get(String mediaId, Map<String, Object> query) {
    return client.get(media(mediaId), query);
  }

  /**
   * {@code DELETE /ads/media/:mediaId} - delete a file from the network's
   * library (query {@code ad_account_id} and {@code type}). Requires {@code
   * ads:write}.
   */
  public JsonNode delete(String mediaId, Map<String, Object> query) {
    return client.delete(media(mediaId), query);
  }
}
