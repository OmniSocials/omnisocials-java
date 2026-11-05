package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * What OmniSocials cannot do, why, and what to do instead: platform rules,
 * features not built yet, features that work in the OmniSocials app only.
 * Accessed via {@code client.limits()}. Read it before you tell a person that
 * something is not possible, so you can give the reason ({@code says}) and
 * the way around it ({@code instead}). When the person still wants a
 * {@code requestable} entry, send it with
 * {@link FeatureRequestsResource#create(Map)} and its {@code limit_id}. No
 * scope needed: every key may call it.
 *
 * <p>Error codes: {@code invalid_platform}, {@code invalid_area} (400).
 */
public final class LimitsResource extends ApiResource {

  public LimitsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /limits} - every entry. Returns
   * {@code { data: Limit[], kinds: { kind: label } }}; a limit is
   * {@code { id, kind, kind_label, area, platforms, title, says, instead,
   * requestable }}. {@code kind} is platform, by_design, not_built, beta,
   * dashboard_only or needs_setup; {@code requestable} is false for platform
   * and by_design.
   */
  public JsonNode list() {
    return client.get("/limits");
  }

  /**
   * {@code GET /limits?platform=&area=} - with filters. Query params:
   * {@code platform} (the entries for one platform; entries for every channel
   * stay in), {@code area} (posts, media, inbox, analytics, automations,
   * accounts, ads, team, other).
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/limits", query);
  }
}
