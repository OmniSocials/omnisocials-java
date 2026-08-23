# OmniSocials Java SDK

The official Java client for the [OmniSocials API](https://docs.omnisocials.com). Schedule and publish posts to Instagram, Facebook, LinkedIn, YouTube, TikTok, X, Pinterest, Bluesky, Threads, Mastodon, and Google Business from one API.

- Java 11+, built on `java.net.http.HttpClient`
- Single runtime dependency: Jackson (`jackson-databind`)
- Automatic retries with exponential backoff, configurable timeouts
- Rich exception hierarchy and a webhook signature verification helper

## Installation

Maven:

```xml
<dependency>
  <groupId>com.omnisocials</groupId>
  <artifactId>omnisocials-java</artifactId>
  <version>0.5.0</version>
</dependency>
```

Gradle:

```groovy
implementation "com.omnisocials:omnisocials-java:0.5.0"
```

## Quickstart

```java
import com.omnisocials.OmniSocials;
import com.omnisocials.Params;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

OmniSocials client = OmniSocials.fromEnv(); // reads OMNISOCIALS_API_KEY
JsonNode post = client.posts().create(Params.builder()
    .put("content", "Hello from the SDK")
    .put("channels", List.of("instagram", "linkedin"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .build());
System.out.println(post.get("data").get("id").asText());
```

## Authentication

Create an API key in the OmniSocials app under **Settings -> API Keys**. Keys look like `omsk_live_...` (or `omsk_test_...`).

The client reads `OMNISOCIALS_API_KEY` from the environment (`OmniSocials.fromEnv()` or a builder without `apiKey`), or you can pass it explicitly:

```java
OmniSocials client = OmniSocials.builder()
    .apiKey("omsk_live_...")
    .build();
```

Building a client without a key throws an `AuthenticationException` right away.

## Configuration

```java
import java.time.Duration;

OmniSocials client = OmniSocials.builder()
    .apiKey("omsk_live_...")
    .baseUrl("https://api.omnisocials.com/v1") // default
    .timeout(Duration.ofSeconds(30))           // per-request timeout (default 30s)
    .maxRetries(2)                             // retries on 429 / 5xx / network errors (default 2)
    .build();
```

Retries use exponential backoff (0.5s, 1s, 2s, ...) with jitter and honor the `Retry-After` header. Other 4xx responses are never retried.

## Rate limits

The API allows **100 requests per minute** per API key. When you exceed it, the SDK retries automatically (respecting `Retry-After`); if retries are exhausted it throws a `RateLimitException` whose `getRetryAfter()` returns the seconds to wait.

## Return values and params

Methods return the parsed response body as-is (Jackson `JsonNode`): single items come back as `{ "data": {...} }`, lists as `{ "data": [...], "pagination": {...} }`, and some responses carry extra sibling keys (media uploads include `compatibility`, PDF uploads include `slides` and `media_ids`, post creates targeting X with a URL in the text include `warnings`). Endpoints that respond `204 No Content` (deletes) return `null`.

Request params are `Map<String, Object>`. Use the fluent `Params` helper, or any map you like:

```java
Map<String, Object> params = Params.builder()
    .put("content", "Hello")
    .put("channels", List.of("instagram"))
    .build();

// Shorthand for flat maps (null values allowed, unlike Map.of):
Map<String, Object> query = Params.of("platform", "instagram", "timezone", "Europe/Amsterdam");
```

## Posts

### Schedule a post

```java
JsonNode res = client.posts().create(Params.builder()
    .put("content", "New drop this Friday")
    .put("channels", List.of("instagram", "facebook", "linkedin"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .put("media_urls", List.of("https://example.com/teaser.jpg"))
    .build());
JsonNode post = res.get("data");
System.out.println(post.get("id").asText() + " " + post.get("status").asText());
```

Omit `scheduled_at` to create a draft. Use a map as `content` for per-platform captions:

```java
client.posts().create(Params.builder()
    .put("content", Params.of(
        "default", "New drop this Friday",
        "x", "New drop this Friday. RT to spread the word"))
    .put("channels", List.of("instagram", "x"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .build());
```

### Publish immediately

```java
client.posts().createAndPublish(Params.builder()
    .put("content", "Going live right now")
    .put("channels", List.of("x", "bluesky"))
    .build());
```

### Per-media alt text

Every `media_urls` / `media_ids` entry accepts either a plain string or a map with an `alt` accessibility description (max 1500 chars). Alt text is delivered to Mastodon (media description), Bluesky (embed alt), X (photos and GIFs), Pinterest (pin alt text), Instagram (images), and LinkedIn (images). Strings and maps can be mixed, and the same shape works in per-platform maps and `thread_parts` media.

```java
client.posts().create(Params.builder()
    .put("content", "Sunrise over the harbor")
    .put("channels", List.of("mastodon", "bluesky"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .put("media_urls", List.of(Params.of(
        "url", "https://example.com/harbor.jpg",
        "alt", "A small sailboat crossing a calm harbor at sunrise, sky in deep orange")))
    .build());
```

### Post with platform-specific options

```java
client.posts().create(Params.builder()
    .put("content", "Behind the scenes of our summer shoot")
    .put("channels", List.of("instagram", "youtube", "x"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .put("media_urls", List.of("https://example.com/bts.mp4"))
    .put("instagram", Params.of("share_to_feed", true))
    .put("youtube", Params.of("title", "Summer shoot BTS", "privacy", "public"))
    .put("x", Params.of("reply_settings", "following", "made_with_ai", false))
    .build());
```

### Chained threads (X, Bluesky, Mastodon, Threads)

Provide 2 to 25 `thread_parts` to publish a chained thread instead of a single tweet. Each part is capped at 280 characters and can carry its own media (`media_ids` / `media_urls`). The same `thread_parts` shape works for `bluesky` (300 chars per part), `mastodon` (500 chars per part) and `threads` (Meta Threads: 2 to 25 parts, 500 characters per part, up to 10 media per part; parts after the first publish as replies to the previous part, and the Threads caption is taken from part 1).

```java
client.posts().create(Params.builder()
    .put("content", "How we grew to 10k followers in 90 days")
    .put("channels", List.of("x"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .put("x", Params.of("thread_parts", List.of(
        Params.of("text", "How we grew to 10k followers in 90 days. A thread:"),
        Params.of("text", "1. We posted every single day, even when it felt pointless."),
        Params.of("text", "2. We replied to every comment within an hour."),
        Params.of("text", "3. Full breakdown on our blog. Link in bio."))))
    .build());
```

```java
// Meta Threads chain with a carousel on the first part
client.posts().create(Params.builder()
    .put("content", "Behind the scenes of our summer shoot")
    .put("channels", List.of("threads"))
    .put("threads", Params.of("thread_parts", List.of(
        Params.of(
            "text", "Behind the scenes of our summer shoot. A few highlights:",
            "media_urls", List.of("https://example.com/shoot-1.jpg", "https://example.com/shoot-2.jpg")),
        Params.of("text", "Day one: scouting locations at sunrise."),
        Params.of("text", "Day two: the full crew, 14 hours, zero regrets."))))
    .build());
```

On update, pass an explicit `thread_parts` of `null` to clear thread mode (revert to a single post); omit it to leave the existing thread untouched. `Params.builder().put("thread_parts", null)` keeps the key and serializes it as JSON null. The same applies to `bluesky`, `mastodon` and `threads`.

### X link posts use credits

X bills API posts whose text contains a URL at a premium, and OmniSocials passes that fee through as prepaid credits (20 credits per URL-containing tweet; threads billed per part with a link). When a create targets X and the text contains a URL, the response carries a top-level `warnings` array (a sibling of `data`):

```java
JsonNode res = client.posts().create(Params.of(
    "content", "Read the full story: https://example.com/post",
    "channels", List.of("x")));
for (JsonNode warning : res.path("warnings")) {
  if ("x_url_post_credits".equals(warning.path("code").asText())) {
    System.out.println(warning.path("credits_required").asInt());
  }
}
```

From `enforce_from` (2026-08-14) the balance is checked at publish time, but credits are only deducted after the post successfully publishes (a failed publish is never charged). If the balance can't cover it, only the X target fails (other platforms publish normally); top up in the dashboard under Settings -> Organisation -> Billing -> Credits, then call `posts().retry(id)`. Posts without links, analytics, and media on X stay free. There is no API endpoint for credits — they are managed in the dashboard.

From 2026-08-14, scheduling an X link post can also be refused up front, before the request is accepted: every scheduled X link post reserves its cost, and a `create`, `update`, or `publish` call that would push the company's total reserved credits past its balance throws an `ApiException` with status `402` and code `x_credits_insufficient`, whose body carries `error.details.credits_required`, `credits_balance`, and `credits_reserved`. Drafts are never gated, and posts scheduled to publish before 2026-08-14 are never gated either.

### List, get, update, publish, retry, delete

```java
JsonNode page = client.posts().list(Params.of("status", "scheduled", "limit", 50));
String id = page.get("data").get(0).get("id").asText();

client.posts().get(id);
client.posts().update(id, Params.of("scheduled_at", "2026-08-02T10:00:00Z"));
client.posts().publish(id);  // publish a draft/scheduled post now
client.posts().retry(id);    // retry only the failed platforms of a failed/warning post
client.posts().delete(id);   // returns null (204)
```

`retry` re-publishes only the platforms that failed, on the same post; platforms that already succeeded are never posted again. It is asynchronous: a 200 means the retry is queued, so poll `get` for the outcome. Max 3 retries per platform.

### Recent platform posts

Fetch recent posts live from the connected platform APIs, including content published outside OmniSocials. Useful for brand-new workspaces where `list()` is empty. Requires the `analytics:read` scope. Each record includes `duration_seconds` (integer, nullable): the video length in whole seconds where the platform reports it — currently TikTok and YouTube; `null` for images and for platforms that don't expose it.

```java
JsonNode recent = client.posts().recentPlatform(
    Params.of("limit", 10, "platforms", List.of("instagram", "x")));
```

## Media

### Upload from a URL (recommended, up to 1GB)

```java
JsonNode upload = client.media().uploadFromUrl(Params.builder()
    .put("url", "https://example.com/launch-video.mp4")
    .put("name", "launch-video-v2")
    .put("folder", "Campaigns")
    .build());
System.out.println(upload.get("data").get("id").asText());
System.out.println(upload.get("compatibility"));
```

Videos over 100MB are processed in the background and come back with status `"processing"`. Every upload response includes a `compatibility` block listing connected platforms that would reject the file.

### Upload a local file (multipart)

```java
import java.nio.file.Path;

// From a path (filename and content type detected from the file)
JsonNode res = client.media().upload(Path.of("photos/product.jpg"), Params.of("name", "product-hero"));

// Or from bytes + filename
byte[] bytes = java.nio.file.Files.readAllBytes(Path.of("photos/product.jpg"));
client.media().upload(bytes, "product.jpg");
```

Direct multipart uploads are capped at 100MB by the CDN; use `uploadFromUrl` or the presigned flow below for bigger files.

### Upload from base64

```java
client.media().uploadFromBase64(Params.builder()
    .put("data", base64String) // no data URI prefix
    .put("mime_type", "image/png")
    .put("filename", "chart.png")
    .build());
```

### PDF carousels

Uploading a PDF rasterizes it into one image slide per page (max 20). The response carries `slides` and `media_ids` alongside `data` (the first slide). Pass ALL of `media_ids`, in order, to `posts().create` to post the deck as a carousel (a native swipeable document on LinkedIn, an image carousel elsewhere).

```java
JsonNode pdf = client.media().uploadFromUrl(Params.of("url", "https://example.com/deck.pdf"));
client.posts().create(Params.builder()
    .put("content", "Our Q3 strategy deck")
    .put("channels", List.of("linkedin"))
    .put("media_ids", pdf.get("media_ids"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .build());
```

### Presigned uploads for large files (up to 1GB)

`createUploadUrl()` mints a one-time upload URL. POST the file to it as multipart form data (field name `file`) within `expires_in_seconds` (600s); the second request needs no auth headers because the single-use token is in the URL. The response of that second request is the created media item (or `media_ids` for a PDF).

```java
JsonNode minted = client.media().createUploadUrl();
String uploadUrl = minted.get("upload_url").asText();
// POST the file to uploadUrl with any HTTP client (multipart field name: "file").
```

### Preflight compatibility check

Check a file against the workspace's connected platforms before uploading. Provide one of `url`, `media_id`, or `size_bytes` + `mime`.

```java
client.media().check(Params.of("url", "https://example.com/huge.mov"));
client.media().check(Params.of("size_bytes", 300_000_000, "mime", "video/quicktime"));
```

### List, get, rename, move, delete

```java
JsonNode items = client.media().list(Params.of("search", "hero", "limit", 20));
String mediaId = items.get("data").get(0).get("id").asText();

client.media().update(mediaId, Params.of("name", "hero-v2", "folder_id", "12"));
client.media().get(mediaId);
client.media().delete(mediaId); // 409 media_in_use if attached to a scheduled post
```

## Folders

```java
JsonNode folders = client.folders().list(); // flat; build the tree via parent_id
JsonNode folder = client.folders().create(Params.of("name", "Campaigns"));
String folderId = folder.get("data").get("id").asText();

client.folders().update(folderId, Params.of("name", "Campaigns 2026"));
client.folders().delete(folderId); // files move to root, subfolders move up
```

## Hashtag Sets

Save reusable hashtag groups and apply them to posts at create time. Uses the `posts:read` / `posts:write` scopes.

```java
JsonNode set = client.hashtagSets().create(Params.of(
    "name", "Launch",
    "hashtags", List.of("saas", "buildinpublic", "startup") // or one string: "#saas #buildinpublic #startup"
));
String setId = set.get("data").get("id").asText();
System.out.println(set.get("data").get("preview").asText()); // "#saas #buildinpublic #startup"

client.hashtagSets().list();
client.hashtagSets().get(setId);
client.hashtagSets().update(setId, Params.of("hashtags", List.of("saas", "founder"))); // replaces the full list
client.hashtagSets().delete(setId); // returns null (204)
```

Apply a set when creating a post with `hashtag_set` (the set name, case-insensitive) or `hashtag_set_id`. The set is applied once at create time and tags already in the caption are skipped. `hashtag_placement` is `"caption_append"` (default) or `"first_comment"`, and `hashtag_platforms` restricts the hashtags to a subset of the post's channels. Instagram's 30-hashtag cap returns error code `hashtag_limit_exceeded`.

```java
client.posts().create(Params.builder()
    .put("content", "Launch day!")
    .put("channels", List.of("instagram", "x"))
    .put("scheduled_at", "2026-08-01T09:00:00Z")
    .put("hashtag_set", "Launch")
    .put("hashtag_placement", "first_comment")
    .put("hashtag_platforms", List.of("instagram"))
    .build());
```

## Accounts

```java
JsonNode accounts = client.accounts().list();
for (JsonNode account : accounts.get("data")) {
  System.out.println(account.get("platform").asText() + " " + account.get("username").asText());
  if (account.path("needs_reconnect").asBoolean(false)) {
    System.out.println("  needs a reconnect: " + account.path("reauth_reason").asText());
  }
}
client.accounts().get(accounts.get("data").get(0).get("id").asText());
```

## Analytics

```java
// One post's latest per-platform metrics
JsonNode stats = client.analytics().post("post_id");
System.out.println(stats.get("data").get("platforms").path("instagram").path("metrics"));

// Batch: up to 100 posts in one call
JsonNode batch = client.analytics().posts(List.of("id1", "id2", "id3"));
// or: client.analytics().posts("id1", "id2", "id3");

// Workspace-wide overview
JsonNode overview = client.analytics().overview(Params.of("period", "30d"));
System.out.println(overview.get("data").get("total_impressions").asLong());

// Account-level stats (followers etc)
JsonNode accountStats = client.analytics().accounts(Params.of("platform", "instagram"));
```

### Best times to post

```java
JsonNode best = client.analytics().bestTimes(
    Params.of("platform", "instagram", "timezone", "Europe/Amsterdam"));
```

## Locations (Instagram place tagging)

```java
JsonNode results = client.locations().search("Griffith Observatory");
String placeId = results.get("data").get(0).get("id").asText();

JsonNode check = client.locations().validate(placeId);
if (check.get("valid").asBoolean()) {
  client.posts().create(Params.builder()
      .put("content", "Golden hour at the observatory")
      .put("channels", List.of("instagram"))
      .put("media_urls", List.of("https://example.com/observatory.jpg"))
      .put("location_id", placeId)
      .put("scheduled_at", "2026-08-01T18:30:00Z")
      .build());
}
```

## Inbox

Read and reply to the social inbox (DMs, comments, and mentions) across connected platforms. Uses the `inbox:read` / `inbox:write` scopes.

```java
JsonNode conversations = client.inbox().listConversations(
    Params.of("platform", "instagram", "unread", true, "limit", 20));
for (JsonNode conversation : conversations.get("data")) {
  System.out.println(conversation.get("conversation_id").asText()
      + " unread=" + conversation.get("unread_count").asInt());
}

String conversationId = conversations.get("data").get(0).get("conversation_id").asText();
client.inbox().getMessages(conversationId, Params.of("limit", 50));
client.inbox().markRead(conversationId);
client.inbox().reply(conversationId, Params.of("text", "Thanks for reaching out!"));
```

Conversation and message lists use cursor pagination (`pagination.next_cursor` / `pagination.has_more`), not the offset pagination used elsewhere in this API. `platform` accepts `instagram`, `facebook`, `linkedin`, `tiktok`, or `x`; a message's `direction` is `"incoming"` or `"outgoing"`. TikTok replies are comments only and capped at 150 characters.

### X DM replies use credits

X only supports the DM conversation type (no comments or mentions). Each X DM reply costs 2 prepaid credits, debited from the company balance before the message is sent and automatically refunded if the send fails:

```java
import com.omnisocials.errors.ApiException;

try {
  client.inbox().reply(conversationId, Params.of("text", "Thanks for the DM!"));
} catch (ApiException e) {
  if (e.getStatus() == 402 && "insufficient_credits".equals(e.getCode())) {
    System.err.println("Not enough credits to send this reply; top up in the dashboard.");
  } else if (e.getStatus() == 402 && "x_inbox_suspended".equals(e.getCode())) {
    System.err.println("This workspace's X inbox is suspended at zero balance; top up and re-enable it.");
  } else {
    throw e;
  }
}
```

A workspace's X inbox is automatically suspended once its credit balance hits zero; DMs that arrive while suspended are not recovered, so top up and re-enable it in the dashboard (Settings -> Organisation -> Billing -> Credits) as soon as possible. Replies on Instagram, Facebook, LinkedIn, and TikTok stay free.

## Webhooks

### Manage endpoints

```java
JsonNode created = client.webhooks().create(Params.builder()
    .put("url", "https://example.com/omnisocials/webhook")
    .put("events", List.of("post.published", "post.failed"))
    .build());
String webhookId = created.get("data").get("id").asText();
System.out.println(created.get("data").get("secret").asText()); // save it, only shown once

client.webhooks().list();
client.webhooks().get(webhookId);
client.webhooks().update(webhookId, Params.of("is_active", false));
JsonNode rotated = client.webhooks().rotateSecret(webhookId); // old secret stops working
client.webhooks().delete(webhookId);
```

### Verify deliveries (Spring Boot example)

Every delivery is signed with your webhook secret. The `X-OmniSocials-Signature` header has the form `t=<unix>,v1=<hex>` where the hex value is an HMAC-SHA256 of `"{timestamp}.{rawBody}"`. Always verify against the RAW request body:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.Webhooks;
import com.omnisocials.errors.WebhookVerificationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OmniSocialsWebhookController {

  private final String secret = System.getenv("OMNISOCIALS_WEBHOOK_SECRET");

  @PostMapping("/omnisocials/webhook")
  public ResponseEntity<Void> handle(
      @RequestBody String rawBody, // Spring gives you the raw body as a String
      @RequestHeader("X-OmniSocials-Signature") String signature) {
    JsonNode event;
    try {
      event = Webhooks.verifySignature(rawBody, signature, secret, 300);
    } catch (WebhookVerificationException e) {
      return ResponseEntity.badRequest().build();
    }

    switch (event.get("type").asText()) {
      case "post.published":
        System.out.println("Published: " + event.get("data").get("post_id").asText());
        break;
      case "post.failed":
        System.err.println("Failed: " + event.get("data").get("post_id").asText());
        break;
      default:
        break;
    }
    return ResponseEntity.ok().build();
  }
}
```

`Webhooks.verifySignature` uses a constant-time comparison (`MessageDigest.isEqual`), rejects timestamps older than the tolerance in seconds (replay protection, default 300), throws `WebhookVerificationException` on any failure, and returns the parsed event on success.

## Health

```java
JsonNode health = client.health(); // { "status": "ok", "version": "1.0.0", "timestamp": "..." }
```

## Error handling

All exceptions thrown by the SDK extend `OmniSocialsException` (unchecked). Non-2xx API responses throw an `ApiException` subclass with `getStatus()`, `getCode()`, `getMessage()`, and the parsed `getBody()`:

| Class | Status | Typical API codes |
|---|---|---|
| `ValidationException` | 400 / 422 | `validation_error`, `platform_not_connected`, `invalid_file_type` |
| `AuthenticationException` | 401 | `unauthorized`, `invalid_api_key` |
| `PermissionDeniedException` | 403 | `forbidden`, `insufficient_scope` |
| `NotFoundException` | 404 | `not_found` |
| `RateLimitException` | 429 | `rate_limit_exceeded` (exposes `getRetryAfter()` seconds) |
| `ServerException` | >= 500 | `internal_error` |
| `ApiConnectionException` | n/a | network failure or timeout |
| `WebhookVerificationException` | n/a | invalid webhook signature |

```java
import com.omnisocials.errors.*;

try {
  client.posts().create(Params.of("content", "Hi", "channels", List.of("instagram")));
} catch (RateLimitException e) {
  System.out.println("Rate limited, retry in " + e.getRetryAfter() + "s");
} catch (ValidationException e) {
  System.err.println("Bad request (" + e.getCode() + "): " + e.getMessage());
  System.err.println(e.getBody());
} catch (ApiConnectionException e) {
  System.err.println("Network problem: " + e.getMessage());
} catch (ApiException e) {
  System.err.println("API error " + e.getStatus() + " (" + e.getCode() + "): " + e.getMessage());
}
```

## API scopes

Each API key carries scopes: `posts:read`, `posts:write`, `media:write`, `accounts:read`, `analytics:read`, `webhooks:manage`. A call with a missing scope throws `PermissionDeniedException` with code `insufficient_scope`.

## Documentation

Full API reference and guides: [https://docs.omnisocials.com](https://docs.omnisocials.com)

## License

MIT
