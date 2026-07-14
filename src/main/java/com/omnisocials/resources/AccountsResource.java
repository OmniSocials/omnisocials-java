package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;

/** Connected social accounts. Accessed via {@code client.accounts()}. */
public final class AccountsResource extends ApiResource {

  public AccountsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /accounts} - the workspace's connected social accounts. */
  public JsonNode list() {
    return client.get("/accounts");
  }

  /** {@code GET /accounts/:id} - a single connected account. */
  public JsonNode get(String id) {
    return client.get("/accounts/" + seg(id));
  }
}
