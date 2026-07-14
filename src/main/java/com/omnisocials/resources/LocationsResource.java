package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Instagram location tagging (Facebook Places). Accessed via {@code client.locations()}. */
public final class LocationsResource extends ApiResource {

  public LocationsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /locations/search?q=} - search Facebook Places for Instagram
   * location tagging. Use a result's {@code id} as {@code location_id} on a post.
   */
  public JsonNode search(String query) {
    return client.get("/locations/search", Map.of("q", query));
  }

  /**
   * {@code GET /locations/validate?id=} - check whether a Facebook Place id is
   * a valid Instagram location before using it as {@code location_id}.
   */
  public JsonNode validate(String id) {
    return client.get("/locations/validate", Map.of("id", id));
  }
}
