package com.omnisocials.resources;

import com.omnisocials.OmniSocials;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Shared base for the SDK's resource classes. */
abstract class ApiResource {

  protected final OmniSocials client;

  ApiResource(OmniSocials client) {
    this.client = client;
  }

  /** URL-encode a single path segment (post/media/webhook ids). */
  protected static String seg(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
