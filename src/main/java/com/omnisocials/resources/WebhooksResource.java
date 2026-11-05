package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Webhook endpoint management. Accessed via {@code client.webhooks()}.
 *
 * <p>For verifying incoming deliveries, see
 * {@link com.omnisocials.Webhooks#verifySignature(String, String, String, long)}.
 */
public final class WebhooksResource extends ApiResource {

  public WebhooksResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /webhooks} - list the workspace's webhook endpoints. */
  public JsonNode list() {
    return client.get("/webhooks");
  }

  /** {@code GET /webhooks/:id} - fetch a single webhook. */
  public JsonNode get(String id) {
    return client.get("/webhooks/" + seg(id));
  }

  /**
   * {@code POST /webhooks} - register an endpoint for event deliveries
   * (post.scheduled, post.published, post.failed, post.approved,
   * post.rejected, ad.status_changed). Params: {@code url}, {@code events}.
   * The {@code data} of an {@code ad.status_changed} delivery is the
   * campaign, not a post: {@code campaign_id}, {@code workspace_id},
   * {@code name}, {@code platform}, {@code campaign_type}, {@code status},
   * {@code previous_status}, {@code external_campaign_id}, {@code budget}.
   * The response includes the signing {@code secret}; save it, it is only
   * shown once.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/webhooks", params);
  }

  /**
   * {@code PATCH /webhooks/:id} - change the {@code url}, {@code events}, or
   * {@code is_active} state.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/webhooks/" + seg(id), params);
  }

  /** {@code DELETE /webhooks/:id} - delete a webhook. Returns {@code null} (204). */
  public JsonNode delete(String id) {
    return client.delete("/webhooks/" + seg(id));
  }

  /**
   * {@code POST /webhooks/:id/rotate-secret} - generate a new signing secret.
   * Save the returned secret; it is only shown once. The old secret stops
   * working immediately.
   */
  public JsonNode rotateSecret(String id) {
    return client.post("/webhooks/" + seg(id) + "/rotate-secret");
  }
}
