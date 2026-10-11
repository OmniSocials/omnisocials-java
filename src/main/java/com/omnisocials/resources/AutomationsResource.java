package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Comment-to-DM automations. Accessed via {@code client.automations()}.
 *
 * <p>An automation answers a keyword in an Instagram comment (or a DM, or a
 * story reply) with a DM, once per person, with an optional public reply under
 * the comment. The scopes are opt-in on the API key: {@code automations:read}
 * for list / get / listEvents, {@code automations:write} for create / update /
 * delete (and for {@code comment_dm_automation} on
 * {@link PostsResource#create(Map)}).
 */
public final class AutomationsResource extends ApiResource {

  public AutomationsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /automations} - list the workspace's comment-to-DM automations.
   * Requires the {@code automations:read} scope (opt-in on the API key).
   */
  public JsonNode list() {
    return client.get("/automations");
  }

  /**
   * {@code GET /automations?status=&post_id=} - list with filters. Query
   * params: {@code status} ({@code active} | {@code paused} |
   * {@code completed}), {@code post_id} (only the automation bound to that
   * OmniSocials post). Requires the {@code automations:read} scope.
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/automations", query);
  }

  /**
   * {@code GET /automations/:id} - fetch a single automation. Requires the
   * {@code automations:read} scope (opt-in on the API key).
   */
  public JsonNode get(String id) {
    return client.get("/automations/" + seg(id));
  }

  /**
   * {@code POST /automations} - create a comment-to-DM automation. Bind it to
   * one post with {@code post_id} (an OmniSocials post that goes to Instagram,
   * scheduled or live; one automation per post) or {@code instagram_media_id}
   * (a post published outside OmniSocials), not both; a {@code trigger} with
   * {@code scope} {@code next_post} or {@code any} needs neither.
   *
   * <p>{@code actions} is required: {@code Map.of("dm", Map.of("text", "..."))}
   * (max 1000 characters) plus an optional {@code public_reply}
   * ({@code enabled}, {@code variations}; comment kind only, up to 10 of 300
   * characters). {@code trigger} is a map of {@code kinds} (one or more of
   * {@code comment}, {@code dm}, {@code story_reply}; default
   * {@code ["comment"]}), {@code scope} ({@code post} default,
   * {@code next_post}, {@code any}), {@code keywords} (up to 20 of 60
   * characters, required unless {@code match} is {@code any}) and
   * {@code match} ({@code word} default, {@code contains}, {@code any}).
   * {@code limits} is a map of {@code max_sends} and {@code stop_at} (both
   * optional). {@code status} is {@code active} (default) or {@code paused}.
   * Instagram rules: one private reply per comment, within 7 days; OmniSocials
   * caps sends at 180 DMs an hour per account.
   *
   * <p>Requires the {@code automations:write} scope (opt-in on the API key).
   * Errors: {@code 400 validation_error}, {@code 400 post_not_instagram},
   * {@code 400 post_required}, {@code 404 post_not_found},
   * {@code 404 instagram_media_not_found}, {@code 409 instagram_not_connected},
   * {@code 409 instagram_needs_reauth}, {@code 409 automation_exists} (the
   * error body also carries {@code automation_id}).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/automations", params);
  }

  /**
   * {@code PATCH /automations/:id} - partial update. Each section
   * ({@code trigger}, {@code actions}, {@code limits}) is merged into the
   * stored one; keys you omit keep their value: new {@code trigger.keywords}
   * keep the kinds, the scope and the match; a new {@code actions.dm.text}
   * keeps the public reply; {@code limits.stop_at} keeps {@code max_sends}.
   * {@code public_reply.enabled: false} or {@code variations: []} turns the
   * public reply off; {@code limits.max_sends: null} removes that limit.
   * {@code status: "paused"} pauses and {@code "active"} resumes; another
   * {@code post_id} / {@code instagram_media_id} moves the automation to
   * that post. A {@code completed} automation starts again when its limits
   * or trigger change. Requires the {@code automations:write} scope.
   * {@code 400 no_fields} when the body is empty.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/automations/" + seg(id), params);
  }

  /**
   * {@code DELETE /automations/:id} - delete an automation. Returns
   * {@code null} (204). Requires the {@code automations:write} scope.
   */
  public JsonNode delete(String id) {
    return client.delete("/automations/" + seg(id));
  }

  /**
   * {@code GET /automations/:id/events} - the automation's activity: one
   * event per person it answered, skipped or failed on. Requires the
   * {@code automations:read} scope.
   *
   * <p>The response is not the plain list envelope: {@code { data: [...],
   * counts: { sent: 12, skipped: 3 }, pagination: {...} }}, where
   * {@code counts} is the number of events per outcome (an outcome with no
   * events is left out). Each event carries {@code kind} ({@code comment},
   * {@code dm}, {@code story_reply}), {@code outcome}, {@code reason} /
   * {@code reason_label}, {@code person} ({@code id}, {@code username}),
   * {@code trigger_id}, {@code trigger_text}, {@code message} (the DM sent),
   * {@code public_reply}, {@code platform_message_id}, {@code created_at}
   * and {@code sent_at}.
   */
  public JsonNode listEvents(String id) {
    return client.get("/automations/" + seg(id) + "/events");
  }

  /**
   * {@code GET /automations/:id/events?outcome=&limit=&offset=} - the
   * activity with filters. Query params: {@code outcome} ({@code sent} |
   * {@code skipped} | {@code failed} | {@code pending}), {@code limit} (1 to
   * 100, default 25), {@code offset}. See {@link #listEvents(String)} for the
   * response shape ({@code data}, {@code counts}, {@code pagination}).
   */
  public JsonNode listEvents(String id, Map<String, Object> query) {
    return client.get("/automations/" + seg(id) + "/events", query);
  }
}
