package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Post tags: the workspace's own labels for its posts (a hook style, a
 * format), to filter and compare posts. Tags never go to a platform.
 * Accessed via {@code client.tags()}. Uses the {@code posts:read} /
 * {@code posts:write} scopes.
 *
 * <p>Set a post's tags with {@code tags} on {@link PostsResource#create(Map)}
 * and {@link PostsResource#update(String, Map)} (tag ids or names; a new name
 * becomes a tag; on update {@code null} or an empty list removes them all),
 * filter with {@code tag} on {@link PostsResource#list(Map)} and
 * {@link AnalyticsResource#overview(Map)}, and compare tags with
 * {@link AnalyticsResource#tags(Map)}.
 *
 * <p>Error codes: {@code tag_not_found} (404), {@code tag_exists} (409, the
 * existing tag is in {@code error.tag}), {@code tag_limit_reached} (409, at most 200 tags per workspace),
 * {@code invalid_tag_name}, {@code invalid_tag_color}, {@code invalid_tags},
 * {@code invalid_post_ids}, {@code too_many_posts} and {@code too_many_tags}
 * (400).
 */
public final class TagsResource extends ApiResource {

  public TagsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /tags} - every tag of the workspace, by name. Each tag:
   * {@code { id, name, color, created_at }}.
   */
  public JsonNode list() {
    return client.get("/tags");
  }

  /**
   * {@code POST /tags} - create a tag. Params: {@code name} (required, max 40
   * characters, unique in the workspace, case-insensitive), {@code color}
   * (blue, green, orange, red, purple, pink, teal, yellow, brown or gray; omit
   * for the next colour of the palette). Returns {@code { data: Tag }} (201).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/tags", params);
  }

  /**
   * {@code PATCH /tags/:id} - rename ({@code name}) and/or recolour
   * ({@code color}). A key you leave out keeps its value; posts keep the tag.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/tags/" + seg(id), params);
  }

  /** {@link #update(String, Map)} with a numeric tag id. */
  public JsonNode update(long id, Map<String, Object> params) {
    return update(Long.toString(id), params);
  }

  /**
   * {@code DELETE /tags/:id} - delete the tag and take it off every post of
   * the workspace (the posts stay). Returns
   * {@code { data: { tag, posts_untagged } }}.
   */
  public JsonNode delete(String id) {
    return client.delete("/tags/" + seg(id));
  }

  /** {@link #delete(String)} with a numeric tag id. */
  public JsonNode delete(long id) {
    return delete(Long.toString(id));
  }

  /**
   * {@code POST /tags/apply} - add, remove or replace the tags of up to 500
   * posts at once, published ones included. Params: {@code post_ids}
   * (required, list of post ids), {@code add}, {@code remove}, {@code set}
   * (lists of tag ids or names; a new name in {@code add} or {@code set}
   * becomes a tag; {@code set} replaces a post's tags). Returns
   * {@code { data: { updated, posts: [{ id, tags }], not_found } }}.
   */
  public JsonNode apply(Map<String, Object> params) {
    return client.post("/tags/apply", params);
  }
}
