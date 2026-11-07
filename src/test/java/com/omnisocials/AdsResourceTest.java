package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisocials.errors.ApiConnectionException;
import com.omnisocials.errors.ServerException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Request path, method and body of the ads calls, and the money-call rule: a
 * call that can start ad spend is never sent twice after a 5xx.
 */
class AdsResourceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String BAD_GATEWAY =
      "{\"error\":{\"code\":\"bad_gateway\",\"message\":\"upstream\"}}";

  /** One recorded request: method, raw path, raw query and body. */
  private static final class Seen {
    final String method;
    final String path;
    final String query;
    final String body;

    Seen(String method, String path, String query, String body) {
      this.method = method;
      this.path = path;
      this.query = query;
      this.body = body;
    }

    JsonNode json() throws IOException {
      return MAPPER.readTree(body);
    }

    /** "METHOD /path?query" as one line. */
    String line() {
      return method + " " + path + (query == null ? "" : "?" + query);
    }
  }

  private static TestServer recording(List<Seen> seen, int status, String body) throws IOException {
    return new TestServer(
        exchange -> {
          String requestBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          seen.add(
              new Seen(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().getRawPath(),
                  exchange.getRequestURI().getRawQuery(),
                  requestBody));
          exchange.getResponseHeaders().set("Retry-After", "0");
          TestServer.respond(exchange, status, body);
        });
  }

  private static OmniSocials client(TestServer server) {
    return OmniSocials.builder()
        .apiKey("omsk_test_key")
        .baseUrl(server.baseUrl())
        .maxRetries(2)
        .build();
  }

  private static Map<String, Object> draftParams() {
    return Params.builder()
        .put("name", "Autumn sale")
        .put("objective", "traffic")
        .put("platforms", List.of("meta", "linkedin"))
        .put("budget", Params.of("amount", 10, "currency", "EUR"))
        .put("schedule", Params.of("duration_days", 7))
        .put("targeting", Params.of("countries", List.of("NL"), "automatic_audience", true))
        .put(
            "ads",
            List.of(
                Params.builder()
                    .put("media", List.of(Params.of("url", "https://example.com/sale.jpg", "type", "image")))
                    .put("primary_text", "30% off everything this week")
                    .put("cta", "shop_now")
                    .put("link", "https://example.com/sale")
                    .build()))
        .build();
  }

  @Test
  void draftCreateUpdateLaunchAndActivateSendTheRightRequests() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 200, "{\"data\":{\"group_id\":\"g-1\"}}")) {
      OmniSocials client = client(server);

      client.ads().drafts().create(draftParams());
      client.ads().drafts().update("g-1", draftParams());
      client.ads().drafts().launch("g-1", 10);
      client.ads().activate(7, 25);

      assertEquals(4, seen.size());

      assertEquals("POST", seen.get(0).method);
      assertEquals("/ads/drafts", seen.get(0).path);
      JsonNode created = seen.get(0).json();
      assertEquals("traffic", created.get("objective").asText());
      assertEquals("linkedin", created.get("platforms").get(1).asText());
      assertEquals(10, created.get("budget").get("amount").asInt());
      assertEquals("shop_now", created.get("ads").get(0).get("cta").asText());

      assertEquals("PUT", seen.get(1).method);
      assertEquals("/ads/drafts/g-1", seen.get(1).path);
      assertEquals(7, seen.get(1).json().get("schedule").get("duration_days").asInt());

      assertEquals("POST", seen.get(2).method);
      assertEquals("/ads/drafts/g-1/launch", seen.get(2).path);
      assertEquals("{\"confirm_budget_amount\":10}", seen.get(2).body);

      assertEquals("POST", seen.get(3).method);
      assertEquals("/ads/campaigns/7/activate", seen.get(3).path);
      assertEquals("{\"confirm_budget_amount\":25}", seen.get(3).body);
    }
  }

  @Test
  void otherCallsSendTheRightRequests() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 200, "{\"data\":{}}")) {
      OmniSocials client = client(server);

      client.ads().drafts().get("g 1/x");
      client.ads().drafts().attachAccount("g-1", "meta", 12);
      client.ads().drafts().launch("g-1", Params.of("confirm_budget_amounts", Params.of("41", 10, "42", 20)));
      client.ads().activateMulti(List.of(7L, 8L), 5);
      client.ads().update(7, Params.of("budget", Params.of("amount", 30, "currency", "EUR"), "confirm_budget_amount", 30));
      client.ads().resume(7);
      client.ads().list(Params.of("status", "active", "limit", 10));

      assertEquals("/ads/drafts/g%201%2Fx", seen.get(0).path);
      assertEquals("{\"platform\":\"meta\",\"ad_account_id\":12}", seen.get(1).body);
      assertEquals(20, seen.get(2).json().get("confirm_budget_amounts").get("42").asInt());
      assertEquals("/ads/campaigns/activate-multi", seen.get(3).path);
      assertEquals("{\"campaign_ids\":[7,8],\"confirm_budget_amount\":5}", seen.get(3).body);
      assertEquals("PATCH", seen.get(4).method);
      assertEquals("/ads/campaigns/7", seen.get(4).path);
      assertEquals("POST", seen.get(5).method);
      assertEquals("/ads/campaigns/7/resume", seen.get(5).path);
      assertEquals("", seen.get(5).body);
      assertEquals("GET", seen.get(6).method);
      assertEquals("/ads/campaigns", seen.get(6).path);
    }
  }

  @Test
  void launchAndActivateAreSentOnceAfterA502() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 502, BAD_GATEWAY)) {
      OmniSocials client = client(server);

      assertThrows(ServerException.class, () -> client.ads().drafts().launch("g-1", 10));
      assertEquals(1, seen.size(), "a launch must not be retried after a 502");

      seen.clear();
      assertThrows(ServerException.class, () -> client.ads().activate(7, 25));
      assertEquals(1, seen.size(), "an activation must not be retried after a 502");

      seen.clear();
      assertThrows(ServerException.class, () -> client.ads().activateMulti(List.of(7L, 8L), 25));
      assertThrows(ServerException.class, () -> client.ads().resume(7));
      assertThrows(
          ServerException.class,
          () -> client.ads().update(7, Params.of("confirm_budget_amount", 30)));
      assertEquals(3, seen.size(), "activate multi, resume and update are each sent once");
    }
  }

  @Test
  void aLaunchWhoseConnectionDropsIsSentOnce() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              requests.incrementAndGet();
              exchange.getRequestBody().readAllBytes();
              exchange.close(); // no answer: the client cannot know what happened
            })) {
      OmniSocials client = client(server);

      assertThrows(ApiConnectionException.class, () -> client.ads().drafts().launch("g-1", 10));
      assertEquals(1, requests.get(), "a launch must not be retried after a connection error");
    }
  }

  @Test
  void aReadIsRetriedAfterA502() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 502, BAD_GATEWAY)) {
      OmniSocials client = client(server);

      assertThrows(ServerException.class, () -> client.ads().capabilities());
      assertEquals(3, seen.size(), "initial attempt + 2 retries");
    }
  }

  @Test
  void aLaunchIsRetriedAfterA429() throws IOException {
    AtomicInteger requests = new AtomicInteger();
    try (TestServer server =
        new TestServer(
            exchange -> {
              exchange.getResponseHeaders().set("Retry-After", "0");
              if (requests.incrementAndGet() == 1) {
                TestServer.respond(
                    exchange,
                    429,
                    "{\"error\":{\"code\":\"rate_limit_exceeded\",\"message\":\"Slow down\"}}");
              } else {
                TestServer.respond(
                    exchange, 200, "{\"data\":{\"group\":{\"group_id\":\"g-1\"},\"results\":[]}}");
              }
            })) {
      JsonNode launched = client(server).ads().drafts().launch("g-1", 10);

      assertEquals(2, requests.get(), "a 429 means nothing started, so it is retried");
      assertEquals("g-1", launched.get("data").get("group").get("group_id").asText());
    }
  }

  // ─── Ads API parity (2026-11-05) ───────────────────────────────────────────

  @Test
  void parityCallsSendTheRightRequests() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 200, "{\"data\":{}}")) {
      com.omnisocials.resources.AdsResource ads = client(server).ads();

      ads.objects().tree(12, Params.of("from", "2026-10-01", "status", List.of("active", "paused")));
      ads.objects().get(12, "ad_set", "23851/1");
      ads.objects().setStatus(12, "ad", "9", "paused", null);
      ads.objects().setStatus(12, "campaign", "238", "active", 25);
      ads.objects().startImport(12);
      ads.bulkStatus(List.of(7L, 8L), "active", 10);
      ads.campaignAds().add(7, List.of(Params.of("primary_text", "Hi", "link", "https://example.com")));
      ads.campaignAds().setStatus(7, 41, "active", 10);
      ads.previews().forAd(7, 41, List.of("feed", "story"));
      ads.targeting().search(Params.of("ad_account_id", 12, "type", "interest", "q", "yoga"));
      ads.audiences().get("aud 1", 12);
      ads.audiences()
          .removeMembers(
              "aud-1",
              Params.of("ad_account_id", 12, "schema", List.of("email"), "rows", List.of(List.of("a@example.com"))));
      ads.leadForms().list(12);
      ads.insights().campaign(7, Params.of("breakdown", "ad"));
      ads.rules().undo(3, 20);
      ads.rules().previewSaved(4);
      ads.results().putMetric("revenue", Params.of("label", "Revenue"));
      ads.media().delete("abc", Params.of("ad_account_id", 12, "type", "image"));
      ads.comments().hide(7, 41, "c:1", false);
      ads.accounts().updateSettings(12, Params.of("spend_cap", null, "confirm_budget_amount", "remove"));
      ads.reachFrequency().reserve("rf1", 12, 500);
      ads.reachFrequency().cancel("rf1", 12);
      ads.budgetSchedules().delete(7, "bs-1");
      ads.google().searchTerms(Params.of("ad_account_id", 12, "limit", 20));

      String[][] expected = {
        {"GET /ads/accounts/12/tree?from=2026-10-01&status=active%2Cpaused", ""},
        {"GET /ads/accounts/12/objects/ad_set/23851%2F1", ""},
        {"PUT /ads/accounts/12/objects/ad/9/status", "{\"status\":\"paused\"}"},
        {"PUT /ads/accounts/12/objects/campaign/238/status", "{\"status\":\"active\",\"confirm_budget_amount\":25}"},
        {"POST /ads/accounts/12/import", ""},
        {"POST /ads/campaigns/bulk-status", "{\"campaign_ids\":[7,8],\"status\":\"active\",\"confirm_budget_amount\":10}"},
        {"POST /ads/campaigns/7/ads", "{\"ads\":[{\"primary_text\":\"Hi\",\"link\":\"https://example.com\"}]}"},
        {"PUT /ads/campaigns/7/ads/41/status", "{\"status\":\"active\",\"confirm_budget_amount\":10}"},
        {"GET /ads/campaigns/7/ads/41/previews?placements=feed%2Cstory", ""},
        {"GET /ads/targeting/search?ad_account_id=12&type=interest&q=yoga", ""},
        {"GET /ads/audiences/aud%201?ad_account_id=12", ""},
        {"DELETE /ads/audiences/aud-1/members", "{\"ad_account_id\":12,\"schema\":[\"email\"],\"rows\":[[\"a@example.com\"]]}"},
        {"GET /ads/lead-forms?ad_account_id=12", ""},
        {"GET /ads/campaigns/7/analytics?breakdown=ad", ""},
        {"POST /ads/rules/activity/3/undo", "{\"confirm_budget_amount\":20}"},
        {"POST /ads/rules/4/preview", ""},
        {"PUT /ads/campaign-results/metrics/revenue", "{\"label\":\"Revenue\"}"},
        {"DELETE /ads/media/abc?ad_account_id=12&type=image", ""},
        {"POST /ads/campaigns/7/ads/41/comments/c%3A1/hide", "{\"hidden\":false}"},
        {"PATCH /ads/accounts/12/settings", "{\"spend_cap\":null,\"confirm_budget_amount\":\"remove\"}"},
        {"POST /ads/reach-frequency/rf1/reserve", "{\"ad_account_id\":12,\"confirm_budget_amount\":500}"},
        {"DELETE /ads/reach-frequency/rf1?ad_account_id=12", ""},
        {"DELETE /ads/campaigns/7/budget-schedules/bs-1", ""},
        {"GET /ads/google/search-terms?ad_account_id=12&limit=20", ""},
      };
      assertEquals(expected.length, seen.size());
      for (int i = 0; i < expected.length; i++) {
        assertEquals(expected[i][0], seen.get(i).line(), "request " + i);
        assertEquals(expected[i][1], seen.get(i).body, "body of " + expected[i][0]);
      }
    }
  }

  @Test
  void parityMoneyCallsAreSentOnceAfterA502() throws IOException {
    List<Seen> seen = Collections.synchronizedList(new ArrayList<>());
    try (TestServer server = recording(seen, 502, BAD_GATEWAY)) {
      com.omnisocials.resources.AdsResource ads = client(server).ads();
      List<Runnable> moneyCalls =
          List.of(
              () -> ads.objects().update(12, "campaign", "238", Params.of("confirm_budget_amount", 30)),
              () -> ads.objects().setStatus(12, "campaign", "238", "active", 25),
              () -> ads.bulkStatus(List.of(7L, 8L), "active", 10),
              () -> ads.campaignAds().setStatus(7, 41, "active", 10),
              () -> ads.rules().create(Params.of("name", "Raise", "confirm_max_daily_budget", 50)),
              () -> ads.rules().update(4, Params.of("enabled", true)),
              () -> ads.rules().undo(3, 20),
              () -> ads.accounts().updateSettings(12, Params.of("spend_cap", 900, "confirm_budget_amount", 900)),
              () -> ads.reachFrequency().reserve("rf1", 12, 500),
              () -> ads.budgetSchedules().create(7, Params.of("confirm_budget_amount", 20)));

      for (int i = 0; i < moneyCalls.size(); i++) {
        seen.clear();
        assertThrows(ServerException.class, moneyCalls.get(i)::run);
        assertEquals(1, seen.size(), "money call " + i + " (" + (seen.isEmpty() ? "?" : seen.get(0).line()) + ") is sent once after a 502");
      }

      seen.clear();
      assertThrows(ServerException.class, () -> ads.objects().tree(12));
      assertEquals(3, seen.size(), "a read is retried: initial attempt + 2 retries");
    }
  }
}
