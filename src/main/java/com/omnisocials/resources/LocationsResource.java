package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Instagram and Threads location tagging. Accessed via {@code client.locations()}. */
public final class LocationsResource extends ApiResource {

  public LocationsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /locations/search?q=} - search Facebook Places for Instagram
   * location tagging (the default {@code instagram} platform). Use a result's
   * {@code id} as {@code location_id} on a post.
   */
  public JsonNode search(String query) {
    return client.get("/locations/search", Map.of("q", query));
  }

  /**
   * {@code GET /locations/search?q=&platform=&latitude=&longitude=} - search
   * locations with full control over the query params. {@code platform} is
   * {@code instagram} (the default) or {@code threads}; the two sources use
   * DIFFERENT ids (a Facebook Place ID is not a Threads location id).
   *
   * <p>Instagram: pass {@code q}. Response:
   * {@code { data: [...], error?, needsPermission? }} ({@code error} is a
   * plain string on the degraded path).
   *
   * <p>Threads: pass {@code q}, or {@code latitude} (-90..90) plus
   * {@code longitude} (-180..180) to search around a point instead of a
   * keyword. Response: {@code { locations: [ { id, name, address, city,
   * country, latitude, longitude } ] }} (all fields but {@code id} nullable),
   * or {@code { error: { code, message } }} with {@code code} one of
   * {@code not_available} (Threads location tagging not enabled in this
   * environment yet), {@code threads_not_connected},
   * {@code threads_reauth_required} (the connection lacks the
   * {@code threads_location_tagging} permission; reconnect Threads), or
   * {@code platform_error}. Validation problems (neither {@code q} nor
   * lat+lng, {@code q} under 2 chars, coordinates out of range) throw a 400.
   * Use a result's {@code id} as {@code threads.location_id} on a post.
   *
   * <p>Threads location tagging is currently rolling out; until Meta approves
   * the permissions it is disabled on production and calls return a clear
   * error.
   */
  public JsonNode search(Map<String, Object> query) {
    return client.get("/locations/search", query);
  }

  /**
   * {@code GET /locations/validate?id=} - check whether a Facebook Place id is
   * a valid Instagram location before using it as {@code location_id}.
   */
  public JsonNode validate(String id) {
    return client.get("/locations/validate", Map.of("id", id));
  }
}
