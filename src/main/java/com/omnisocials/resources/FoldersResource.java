package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Media library folders. Accessed via {@code client.folders()}. */
public final class FoldersResource extends ApiResource {

  public FoldersResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /folders} - all folders (flat list; build the tree via {@code parent_id}). */
  public JsonNode list() {
    return client.get("/folders");
  }

  /**
   * {@code POST /folders} - create a folder (optionally nested under
   * {@code parent_id}). Params: {@code name} (required), {@code parent_id}.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/folders", params);
  }

  /**
   * {@code PATCH /folders/:id} - rename ({@code name}) and/or move
   * ({@code parent_id}, or {@code null} for the top level). Cycles are rejected.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/folders/" + seg(id), params);
  }

  /**
   * {@code DELETE /folders/:id} - delete a folder. Media is preserved: files
   * move to the root and subfolders move up to the deleted folder's parent.
   * Returns {@code null} (204).
   */
  public JsonNode delete(String id) {
    return client.delete("/folders/" + seg(id));
  }
}
