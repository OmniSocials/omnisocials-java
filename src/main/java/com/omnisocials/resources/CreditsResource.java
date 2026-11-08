package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;

/**
 * The organisation's credit balance and what X statistics cost. Accessed via
 * {@code client.credits()}. Top-ups are made in the OmniSocials app
 * (Settings -> Organisation -> Billing -> Credits).
 */
public final class CreditsResource extends ApiResource {

  public CreditsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /credits} - the balance (1 credit = EUR 0.01),
   * {@code auto_refill} ({@code { enabled }}), {@code this_month} (credits used
   * per reason) and {@code x_statistics}: {@code state}, {@code paused_at},
   * {@code charging} ({@code { active, from }}), {@code prices}
   * ({@code { post_read, media_read, follower_read }}),
   * {@code total_credits_month}, {@code this_month} ({@code { post_reads,
   * media_reads, follower_reads, refreshes, credits }}) and {@code accounts}
   * ({@code [{ account_id, workspace_id, handle, posts, followers, tweets_30d,
   * credits_month }]}). Needs the opt-in scope {@code credits:read} and
   * billing rights (manage_billing) for the key's creator; OmniSocials Connect
   * keys get 403 {@code not_available_for_connect}.
   */
  public JsonNode get() {
    return client.get("/credits");
  }
}
