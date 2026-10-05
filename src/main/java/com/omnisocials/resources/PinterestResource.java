package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Pinterest product Pins for product tagging. Accessed via {@code client.pinterest()}. */
public final class PinterestResource extends ApiResource {

  public PinterestResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /pinterest/products} - list the product Pins of the connected
   * Pinterest account, from the source the API picks: {@code catalog} when
   * the connection has catalog access, else {@code pins}. Use a result's
   * {@code pin_id} in {@code product_tags} inside the {@code pinterest} map
   * of a post.
   */
  public JsonNode listProducts() {
    return client.get("/pinterest/products");
  }

  /**
   * {@code GET /pinterest/products?source=&product_group_id=&bookmark=&page_size=}
   * - list the product Pins of the connected Pinterest account with full
   * control over the query params. Use a result's {@code pin_id} in
   * {@code product_tags} inside the {@code pinterest} map of a post to tag the
   * product on the Pin (max 24 per Pin). Pinterest only accepts a product Pin
   * that is public, belongs to the same account and links to a website that
   * account claimed; products of other merchants cannot be tagged.
   *
   * <p>{@code source} is {@code catalog} or {@code pins}. {@code catalog}
   * reads the Pinterest catalog (with {@code price}, {@code currency},
   * {@code availability}, {@code item_id}) and needs catalog access, which is
   * given one time in the OmniSocials composer (Pinterest options, Add
   * products, Connect catalog); {@code product_group_id} and
   * {@code page_size} (1..100, default 25) apply to this source only.
   * {@code pins} reads the account's own Pins and works on every connection;
   * one call scans up to 250 Pins, so {@code products} can be empty while
   * {@code bookmark} is set (call again with the bookmark).
   *
   * <p>Response (not the usual {@code data} envelope): {@code { products: [ {
   * pin_id, title, description, link, image_url, price, currency,
   * availability, item_id } ], bookmark, source, catalog_access }} plus
   * {@code product_groups} and {@code product_group_id} for the catalog
   * source, or {@code { error: { code, message } }} without {@code products}
   * when the list could not be read, both with HTTP 200. {@code code} is one
   * of {@code pinterest_not_connected},
   * {@code pinterest_catalog_access_required} or {@code platform_error}. A
   * bad {@code source} or {@code product_group_id} throws a 400.
   */
  public JsonNode listProducts(Map<String, Object> query) {
    return client.get("/pinterest/products", query);
  }

  /**
   * {@code GET /pinterest/products/validate?id=} - check whether a Pin can be
   * used in {@code product_tags} before creating the post. {@code id} is a Pin
   * id or a Pin link ({@code https://www.pinterest.com/pin/<id>/}). Response:
   * {@code { valid, pin_id, ... }}; {@code unverified: true} means the check
   * could not run and the publish step is the final check.
   */
  public JsonNode validateProduct(String id) {
    return client.get("/pinterest/products/validate", Map.of("id", id));
  }
}
