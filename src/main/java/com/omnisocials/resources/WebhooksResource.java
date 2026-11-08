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

  /**
   * {@code GET /webhooks} - list the webhooks of the key's workspace and the
   * organisation-wide webhooks of its organisation ({@code workspace_id}
   * null, made in the OmniSocials dashboard; they receive the events of
   * every workspace). A webhook whose creator lost access to the
   * organisation or workspace is switched off
   * automatically: {@code is_active} is false and {@code last_failure.error}
   * says why.
   */
  public JsonNode list() {
    return client.get("/webhooks");
  }

  /**
   * {@code GET /webhooks/:id} - fetch a webhook of the key's workspace, or an
   * organisation-wide webhook of its organisation ({@code workspace_id} null).
   */
  public JsonNode get(String id) {
    return client.get("/webhooks/" + seg(id));
  }

  /**
   * {@code POST /webhooks} - register an endpoint for event deliveries
   * (post.scheduled, post.published, post.failed, post.approved, post.rejected,
   * and for ads ad.status_changed, ad.created, ad.updated, ad.review,
   * ad.delivery_issue, campaign.created, campaign.updated, campaign.budget,
   * lead.received, ads.sync_failed, ads.sync_recovered,
   * ads.initial_sync_completed, ad_rule.triggered). Params: {@code url},
   * {@code events}. The {@code data} of an {@code ad.status_changed} delivery
   * is the campaign, not a post: {@code campaign_id}, {@code workspace_id},
   * {@code name}, {@code platform}, {@code campaign_type}, {@code status},
   * {@code previous_status}, {@code external_campaign_id}, {@code budget}; the
   * other campaign events carry the campaign plus {@code details}, the rest
   * their own {@code data}. Any Ads event (ad.*, campaign.*, lead.*, ads.*,
   * ad_rule.*) needs the {@code ads:read} scope on the key and
   * {@code access_ads} for its person (403 {@code insufficient_scope} / 403
   * {@code insufficient_permission}). The response includes the signing
   * {@code secret}; save it, it is only shown once.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/webhooks", params);
  }

  /**
   * {@code PATCH /webhooks/:id} - change the {@code url}, {@code events}, or
   * {@code is_active} state. Works on organisation-wide webhooks too.
   * {@code is_active: true} switches on a webhook that was switched off
   * (also an automatic switch-off); setting {@code url}, {@code events} or
   * switching it on makes the key's creator the person behind the webhook.
   * Setting {@code url} on an organisation-wide webhook (even its current
   * one) moves it to the key's workspace: from then on it gets only that
   * workspace's events, and the answer shows the new {@code workspace_id}.
   * When the webhook has Ads events after the change, setting {@code url} or
   * {@code events} or switching it on needs the {@code ads:read} scope and
   * {@code access_ads} for the key's creator (403 otherwise, nothing
   * changed).
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/webhooks/" + seg(id), params);
  }

  /**
   * {@code DELETE /webhooks/:id} - delete a webhook of the key's workspace or
   * an organisation-wide webhook of its organisation. Returns {@code null}
   * (204).
   */
  public JsonNode delete(String id) {
    return client.delete("/webhooks/" + seg(id));
  }

  /**
   * {@code POST /webhooks/:id/rotate-secret} - generate a new signing secret.
   * Save the returned secret; it is only shown once. The old secret keeps
   * signing for 24 hours: every delivery then carries
   * {@code X-OmniSocials-Signature} (new secret) and
   * {@code X-OmniSocials-Signature-Previous} (old secret, same format, same
   * timestamp and body). Verify the first with the new secret and, when that
   * fails, the second with the old one (see
   * {@link com.omnisocials.Webhooks#verifySignature(String, String, String)}).
   * Rotating again within the 24 hours replaces the previous secret with the
   * one just rotated out. Works on organisation-wide webhooks too.
   */
  public JsonNode rotateSecret(String id) {
    return client.post("/webhooks/" + seg(id) + "/rotate-secret");
  }
}
