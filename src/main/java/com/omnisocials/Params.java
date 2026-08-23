package com.omnisocials;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small fluent helper for building {@code Map<String, Object>} request params.
 *
 * <pre>{@code
 * Map<String, Object> params = Params.builder()
 *     .put("content", "Hello from Java")
 *     .put("channels", List.of("instagram", "linkedin"))
 *     .put("scheduled_at", "2026-08-01T09:00:00Z")
 *     .build();
 * }</pre>
 *
 * <p>Unlike {@link Map#of}, {@code null} values are allowed and are serialized
 * as JSON {@code null} (some endpoints use explicit nulls, for example
 * {@code thread_parts: null} inside the {@code x}, {@code bluesky},
 * {@code mastodon} or {@code threads} options map clears thread mode on a
 * post update). Values can
 * be strings, numbers, booleans, {@code List}s, or nested {@code Map}s.
 */
public final class Params {

  private Params() {}

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Build a params map from alternating key/value pairs:
   * {@code Params.of("platform", "instagram", "timezone", "Europe/Amsterdam")}.
   */
  public static Map<String, Object> of(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Params.of expects an even number of arguments (key/value pairs), got "
              + keyValues.length + ": " + Arrays.toString(keyValues));
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      Object key = keyValues[i];
      if (!(key instanceof String)) {
        throw new IllegalArgumentException(
            "Params.of keys must be strings, got " + key + " at position " + i);
      }
      map.put((String) key, keyValues[i + 1]);
    }
    return map;
  }

  /** Fluent builder returned by {@link Params#builder()}. */
  public static final class Builder {

    private final Map<String, Object> map = new LinkedHashMap<>();

    private Builder() {}

    /** Add one param. {@code null} values are kept and serialized as JSON null. */
    public Builder put(String key, Object value) {
      map.put(key, value);
      return this;
    }

    /** Add all entries from another map. */
    public Builder putAll(Map<String, ?> other) {
      map.putAll(other);
      return this;
    }

    public Map<String, Object> build() {
      return new LinkedHashMap<>(map);
    }
  }
}
