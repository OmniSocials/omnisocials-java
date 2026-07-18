package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Instagram Reels audio (Meta's licensed catalog). Accessed via {@code client.audio()}. */
public final class AudioResource extends ApiResource {

  public AudioResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /audio/search} - trending audio from Meta's licensed catalog for
   * Instagram Reels (no keyword, default {@code music} type). Use a result's
   * {@code audio_id} as {@code instagram.audio_id} on a reel post (with optional
   * {@code instagram.audio_volume} / {@code instagram.video_volume}, integers
   * 0-100). {@code preview_url} is a temporary URL (~1.5 days); never persist it.
   */
  public JsonNode search() {
    return client.get("/audio/search");
  }

  /**
   * {@code GET /audio/search?q=} - search Meta's licensed audio catalog by song
   * or artist keyword for Instagram Reels (default {@code music} type). Use a
   * result's {@code audio_id} as {@code instagram.audio_id} on a reel post.
   */
  public JsonNode search(String query) {
    return client.get("/audio/search", Map.of("q", query));
  }

  /**
   * {@code GET /audio/search?q=&type=} - search Meta's licensed audio catalog
   * for Instagram Reels. Query params: {@code q} (optional; omit for trending
   * audio), {@code type} ({@code music}, the default, or
   * {@code original_sound}).
   */
  public JsonNode search(Map<String, Object> query) {
    return client.get("/audio/search", query);
  }
}
