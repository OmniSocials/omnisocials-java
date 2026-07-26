package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Saved, reusable hashtag groups. Accessed via {@code client.hashtagSets()}.
 *
 * <p>Apply a set to a post at create time by passing {@code hashtag_set}
 * (name, case-insensitive) or {@code hashtag_set_id} to
 * {@link PostsResource#create(Map)}.
 */
public final class HashtagSetsResource extends ApiResource {

  public HashtagSetsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /hashtag-sets} - list the workspace's saved hashtag sets. */
  public JsonNode list() {
    return client.get("/hashtag-sets");
  }

  /** {@code GET /hashtag-sets/:id} - fetch a single hashtag set. */
  public JsonNode get(String id) {
    return client.get("/hashtag-sets/" + seg(id));
  }

  /**
   * {@code POST /hashtag-sets} - create a hashtag set. Params: {@code name}
   * (required), {@code hashtags} (required; a list of tags, or a single
   * string of tags).
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/hashtag-sets", params);
  }

  /**
   * {@code PATCH /hashtag-sets/:id} - rename ({@code name}) and/or replace
   * the tags ({@code hashtags} replaces the FULL list).
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/hashtag-sets/" + seg(id), params);
  }

  /** {@code DELETE /hashtag-sets/:id} - delete a hashtag set. Returns {@code null} (204). */
  public JsonNode delete(String id) {
    return client.delete("/hashtag-sets/" + seg(id));
  }
}
