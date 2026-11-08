package com.omnisocials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.omnisocials.errors.ApiConnectionException;
import com.omnisocials.errors.ApiException;
import com.omnisocials.errors.AuthenticationException;
import com.omnisocials.errors.OmniSocialsException;
import com.omnisocials.resources.AccountsResource;
import com.omnisocials.resources.AdsResource;
import com.omnisocials.resources.AnalyticsResource;
import com.omnisocials.resources.ApprovalWorkflowsResource;
import com.omnisocials.resources.AudioResource;
import com.omnisocials.resources.CreditsResource;
import com.omnisocials.resources.AutomationsResource;
import com.omnisocials.resources.FeatureRequestsResource;
import com.omnisocials.resources.FoldersResource;
import com.omnisocials.resources.HashtagSetsResource;
import com.omnisocials.resources.InboxResource;
import com.omnisocials.resources.LimitsResource;
import com.omnisocials.resources.LocationsResource;
import com.omnisocials.resources.MediaResource;
import com.omnisocials.resources.PinterestResource;
import com.omnisocials.resources.PostsResource;
import com.omnisocials.resources.TagsResource;
import com.omnisocials.resources.WebhooksResource;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The OmniSocials API client.
 *
 * <pre>{@code
 * OmniSocials client = OmniSocials.fromEnv(); // reads OMNISOCIALS_API_KEY
 * // or:
 * OmniSocials client = OmniSocials.builder()
 *     .apiKey("omsk_live_...")
 *     .build();
 *
 * JsonNode post = client.posts().create(Params.builder()
 *     .put("content", "Hello from the SDK")
 *     .put("channels", List.of("instagram", "linkedin"))
 *     .put("scheduled_at", "2026-08-01T09:00:00Z")
 *     .build());
 * }</pre>
 *
 * <p>Methods return the parsed response body as-is ({@link JsonNode}); the API
 * wraps results in {@code { data, pagination?, ... }}. Endpoints that respond
 * {@code 204 No Content} return {@code null}. Non-2xx responses throw an
 * {@link ApiException} subclass; network failures throw
 * {@link ApiConnectionException}. 429 / 5xx / connection errors are retried
 * automatically with exponential backoff and jitter, honoring
 * {@code Retry-After}. A call that can start or raise ad spend is retried on a
 * 429 only (see {@link RetryPolicy#RATE_LIMIT_ONLY}).
 */
public final class OmniSocials {

  /** SDK version, also used in the User-Agent header. */
  public static final String VERSION = "0.18.0";

  public static final String DEFAULT_BASE_URL = "https://api.omnisocials.com/v1";
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
  public static final int DEFAULT_MAX_RETRIES = 2;

  private static final String USER_AGENT = "omnisocials-java/" + VERSION;
  private static final String API_KEY_ENV_VAR = "OMNISOCIALS_API_KEY";

  private final String apiKey;
  private final String baseUrl;
  private final Duration timeout;
  private final int maxRetries;
  private final HttpClient httpClient;
  private final ObjectMapper mapper = new ObjectMapper();

  private final PostsResource posts;
  private final MediaResource media;
  private final FoldersResource folders;
  private final HashtagSetsResource hashtagSets;
  private final TagsResource tags;
  private final ApprovalWorkflowsResource approvalWorkflows;
  private final AccountsResource accounts;
  private final AnalyticsResource analytics;
  private final CreditsResource credits;
  private final AudioResource audio;
  private final LocationsResource locations;
  private final PinterestResource pinterest;
  private final AutomationsResource automations;
  private final AdsResource ads;
  private final InboxResource inbox;
  private final WebhooksResource webhooks;
  private final LimitsResource limits;
  private final FeatureRequestsResource featureRequests;

  private OmniSocials(Builder builder, String apiKey) {
    this.apiKey = apiKey;
    this.baseUrl = builder.baseUrl.replaceAll("/+$", "");
    this.timeout = builder.timeout;
    this.maxRetries = builder.maxRetries;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(builder.timeout)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    this.posts = new PostsResource(this);
    this.media = new MediaResource(this);
    this.folders = new FoldersResource(this);
    this.hashtagSets = new HashtagSetsResource(this);
    this.tags = new TagsResource(this);
    this.approvalWorkflows = new ApprovalWorkflowsResource(this);
    this.accounts = new AccountsResource(this);
    this.analytics = new AnalyticsResource(this);
    this.credits = new CreditsResource(this);
    this.audio = new AudioResource(this);
    this.locations = new LocationsResource(this);
    this.pinterest = new PinterestResource(this);
    this.automations = new AutomationsResource(this);
    this.ads = new AdsResource(this);
    this.inbox = new InboxResource(this);
    this.webhooks = new WebhooksResource(this);
    this.limits = new LimitsResource(this);
    this.featureRequests = new FeatureRequestsResource(this);
  }

  /** Start building a client. */
  public static Builder builder() {
    return new Builder();
  }

  /** Build a client from the {@code OMNISOCIALS_API_KEY} environment variable. */
  public static OmniSocials fromEnv() {
    return builder().build();
  }

  // ─── Resources ─────────────────────────────────────────────────────────────

  public PostsResource posts() {
    return posts;
  }

  public MediaResource media() {
    return media;
  }

  public FoldersResource folders() {
    return folders;
  }

  public HashtagSetsResource hashtagSets() {
    return hashtagSets;
  }

  public TagsResource tags() {
    return tags;
  }

  public ApprovalWorkflowsResource approvalWorkflows() {
    return approvalWorkflows;
  }

  public AccountsResource accounts() {
    return accounts;
  }

  public AnalyticsResource analytics() {
    return analytics;
  }

  /** The organisation's credit balance and what X statistics cost (scope {@code credits:read}). */
  public CreditsResource credits() {
    return credits;
  }

  public AudioResource audio() {
    return audio;
  }

  public LocationsResource locations() {
    return locations;
  }

  public PinterestResource pinterest() {
    return pinterest;
  }

  public AutomationsResource automations() {
    return automations;
  }

  public AdsResource ads() {
    return ads;
  }

  public InboxResource inbox() {
    return inbox;
  }

  public WebhooksResource webhooks() {
    return webhooks;
  }

  /** What OmniSocials cannot do, why, and what to do instead. */
  public LimitsResource limits() {
    return limits;
  }

  /** Feature requests to the OmniSocials team, for the person who owns the key. */
  public FeatureRequestsResource featureRequests() {
    return featureRequests;
  }

  /** {@code GET /health} (no scopes required). */
  public JsonNode health() {
    return get("/health");
  }

  // ─── HTTP verb helpers used by resources ───────────────────────────────────
  // Public so the resource classes (and integrators calling endpoints not yet
  // wrapped) can use them; most callers should go through the resources.

  public JsonNode get(String path) {
    return request("GET", path, null, null, null, RetryPolicy.DEFAULT);
  }

  public JsonNode get(String path, Map<String, Object> query) {
    return request("GET", path, query, null, null, RetryPolicy.DEFAULT);
  }

  public JsonNode post(String path) {
    return post(path, RetryPolicy.DEFAULT);
  }

  /** POST without a body, retried as {@code retry} says. */
  public JsonNode post(String path, RetryPolicy retry) {
    return request("POST", path, null, null, null, retry);
  }

  public JsonNode post(String path, Map<String, Object> body) {
    return post(path, body, RetryPolicy.DEFAULT);
  }

  /**
   * POST a JSON body, retried as {@code retry} says. A call that can start ad
   * spend passes {@link RetryPolicy#RATE_LIMIT_ONLY}.
   */
  public JsonNode post(String path, Map<String, Object> body, RetryPolicy retry) {
    return request("POST", path, null, body == null ? Map.of() : body, null, retry);
  }

  public JsonNode patch(String path, Map<String, Object> body) {
    return patch(path, body, RetryPolicy.DEFAULT);
  }

  /** PATCH a JSON body, retried as {@code retry} says. */
  public JsonNode patch(String path, Map<String, Object> body, RetryPolicy retry) {
    return request("PATCH", path, null, body == null ? Map.of() : body, null, retry);
  }

  /** PUT a JSON body: it replaces the whole resource. */
  public JsonNode put(String path, Map<String, Object> body) {
    return put(path, body, RetryPolicy.DEFAULT);
  }

  /** PUT a JSON body, retried as {@code retry} says. */
  public JsonNode put(String path, Map<String, Object> body, RetryPolicy retry) {
    return request("PUT", path, null, body == null ? Map.of() : body, null, retry);
  }

  public JsonNode delete(String path) {
    return request("DELETE", path, null, null, null, RetryPolicy.DEFAULT);
  }

  /** DELETE with query params (for example {@code ad_account_id}). */
  public JsonNode delete(String path, Map<String, Object> query) {
    return request("DELETE", path, query, null, null, RetryPolicy.DEFAULT);
  }

  /** DELETE with query params and a JSON body (removing people from a customer list). */
  public JsonNode delete(String path, Map<String, Object> query, Map<String, Object> body) {
    return request("DELETE", path, query, body, null, RetryPolicy.DEFAULT);
  }

  /** POST a multipart/form-data body with one "file" part plus optional text fields. */
  public JsonNode postMultipart(
      String path,
      byte[] fileBytes,
      String filename,
      String fileContentType,
      Map<String, Object> fields) {
    Objects.requireNonNull(fileBytes, "fileBytes");
    MultipartBody form = MultipartBody.build(fileBytes, filename, fileContentType, fields);
    return request("POST", path, null, null, form, RetryPolicy.DEFAULT);
  }

  // ─── Core request loop ─────────────────────────────────────────────────────

  private JsonNode request(
      String method,
      String path,
      Map<String, Object> query,
      Map<String, Object> jsonBody,
      MultipartBody multipart,
      RetryPolicy retry) {
    boolean rateLimitOnly = retry == RetryPolicy.RATE_LIMIT_ONLY;
    URI uri = buildUri(path, query);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("Authorization", "Bearer " + apiKey)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json");

    HttpRequest.BodyPublisher publisher;
    if (multipart != null) {
      requestBuilder.header("Content-Type", multipart.contentType());
      publisher = HttpRequest.BodyPublishers.ofByteArray(multipart.bytes);
    } else if (jsonBody != null) {
      requestBuilder.header("Content-Type", "application/json");
      try {
        publisher =
            HttpRequest.BodyPublishers.ofString(
                mapper.writeValueAsString(jsonBody), StandardCharsets.UTF_8);
      } catch (JsonProcessingException e) {
        throw new OmniSocialsException("Failed to serialize the request body to JSON.", e);
      }
    } else {
      publisher = HttpRequest.BodyPublishers.noBody();
    }
    HttpRequest httpRequest = requestBuilder.method(method, publisher).build();

    int attempt = 0;
    while (true) {
      HttpResponse<String> response;
      try {
        response =
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      } catch (IOException e) {
        // Network failure or timeout: retryable, unless the call may have
        // started ad spend.
        if (!rateLimitOnly && attempt < maxRetries) {
          sleep(backoffMillis(attempt, null));
          attempt++;
          continue;
        }
        boolean timedOut = e instanceof HttpTimeoutException;
        throw new ApiConnectionException(
            timedOut
                ? "Request timed out after " + timeout.toMillis() + "ms (" + method + " " + uri + ")."
                : "Connection error (" + method + " " + uri + "): " + e.getMessage(),
            e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ApiConnectionException("Request interrupted (" + method + " " + uri + ").", e);
      }

      int status = response.statusCode();
      if (status >= 200 && status < 300) {
        if (status == 204) {
          return null;
        }
        String text = response.body();
        if (text == null || text.isEmpty()) {
          return null;
        }
        try {
          return mapper.readTree(text);
        } catch (JsonProcessingException e) {
          return TextNode.valueOf(text);
        }
      }

      Long retryAfter = parseRetryAfter(response.headers().firstValue("retry-after").orElse(null));
      boolean retryable = status == 429 || (status >= 500 && !rateLimitOnly);
      if (retryable && attempt < maxRetries) {
        sleep(backoffMillis(attempt, retryAfter));
        attempt++;
        continue;
      }

      throw responseToException(status, response.body(), retryAfter);
    }
  }

  // ─── Internals ─────────────────────────────────────────────────────────────

  private URI buildUri(String path, Map<String, Object> query) {
    String fullPath = path.startsWith("/") ? path : "/" + path;
    // Every id in the path was encoded with ApiResource.seg, which leaves "."
    // and ".." as they are: URL normalisation drops such a segment or climbs
    // one level, so an id of ".." would reach another endpoint of the same API
    // key, and an empty id the list route. Refused before anything is sent.
    String[] segments = fullPath.split("\\?", 2)[0].split("/", -1);
    for (int i = 1; i < segments.length; i++) {
      String segment = segments[i];
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException(
            "Invalid request path \"" + fullPath + "\": an id cannot be empty, \".\" or \"..\".");
      }
    }
    String url = baseUrl + fullPath;
    if (query != null && !query.isEmpty()) {
      StringBuilder qs = new StringBuilder();
      for (Map.Entry<String, Object> entry : query.entrySet()) {
        Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        String serialized;
        if (value instanceof Collection) {
          serialized =
              ((Collection<?>) value)
                  .stream().map(String::valueOf).collect(Collectors.joining(","));
        } else {
          serialized = String.valueOf(value);
        }
        if (qs.length() > 0) {
          qs.append('&');
        }
        qs.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
            .append('=')
            .append(URLEncoder.encode(serialized, StandardCharsets.UTF_8));
      }
      if (qs.length() > 0) {
        url += "?" + qs;
      }
    }
    return URI.create(url);
  }

  /** Exponential backoff (0.5s, 1s, 2s, ...) with jitter; Retry-After wins. */
  private long backoffMillis(int attempt, Long retryAfterSeconds) {
    if (retryAfterSeconds != null) {
      return Math.min(retryAfterSeconds, 60L) * 1000L;
    }
    double base = 500.0 * Math.pow(2, attempt);
    double jitter = 0.75 + ThreadLocalRandom.current().nextDouble() * 0.5; // 75% - 125%
    return (long) Math.min(base * jitter, 30_000.0);
  }

  private static void sleep(long millis) {
    if (millis <= 0) {
      return;
    }
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiConnectionException("Interrupted while waiting to retry.", e);
    }
  }

  /** Parse a Retry-After header value (delta-seconds or HTTP-date) to seconds. */
  private static Long parseRetryAfter(String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      long seconds = Long.parseLong(value.trim());
      return seconds >= 0 ? seconds : null;
    } catch (NumberFormatException ignored) {
      // Fall through to HTTP-date parsing.
    }
    try {
      ZonedDateTime date = ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
      long diff = date.toEpochSecond() - (System.currentTimeMillis() / 1000L);
      return Math.max(diff, 0L);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private ApiException responseToException(int status, String bodyText, Long retryAfter) {
    JsonNode parsedBody = null;
    String code = null;
    String message = "Request failed with status " + status + ".";
    if (bodyText != null && !bodyText.isEmpty()) {
      try {
        parsedBody = mapper.readTree(bodyText);
        JsonNode error = parsedBody.get("error");
        if (error != null && error.isObject()) {
          if (error.hasNonNull("code") && error.get("code").isTextual()) {
            code = error.get("code").asText();
          }
          if (error.hasNonNull("message") && error.get("message").isTextual()) {
            message = error.get("message").asText();
          }
        } else if (error != null && error.isTextual()) {
          message = error.asText();
        }
      } catch (JsonProcessingException ignored) {
        // Non-JSON error body (e.g. an HTML 413 page from the CDN).
      }
    }
    return ApiException.fromStatus(status, code, message, parsedBody, retryAfter);
  }

  // ─── Builder ───────────────────────────────────────────────────────────────

  /** Builder for {@link OmniSocials}. */
  public static final class Builder {

    private String apiKey;
    private String baseUrl = DEFAULT_BASE_URL;
    private Duration timeout = DEFAULT_TIMEOUT;
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private Function<String, String> envLookup = System::getenv;

    private Builder() {}

    /**
     * API key ({@code omsk_live_*}). When not set, the
     * builder falls back to the {@code OMNISOCIALS_API_KEY} environment
     * variable.
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /** API base URL. Defaults to {@value OmniSocials#DEFAULT_BASE_URL}. */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
      return this;
    }

    /** Per-request timeout. Defaults to 30 seconds. */
    public Builder timeout(Duration timeout) {
      this.timeout = Objects.requireNonNull(timeout, "timeout");
      return this;
    }

    /** Automatic retries on 429 / 5xx / connection errors. Defaults to 2. */
    public Builder maxRetries(int maxRetries) {
      if (maxRetries < 0) {
        throw new IllegalArgumentException("maxRetries must be >= 0, got " + maxRetries);
      }
      this.maxRetries = maxRetries;
      return this;
    }

    /** Test hook: override how environment variables are read. */
    Builder envLookup(Function<String, String> envLookup) {
      this.envLookup = Objects.requireNonNull(envLookup, "envLookup");
      return this;
    }

    /**
     * Build the client.
     *
     * @throws AuthenticationException when no API key was provided and the
     *     {@code OMNISOCIALS_API_KEY} environment variable is not set
     */
    public OmniSocials build() {
      String key = apiKey != null ? apiKey : envLookup.apply(API_KEY_ENV_VAR);
      if (key == null || key.isEmpty()) {
        throw new AuthenticationException(
            401,
            "missing_api_key",
            "No API key provided. Pass one with OmniSocials.builder().apiKey(\"omsk_live_...\") "
                + "or set the OMNISOCIALS_API_KEY environment variable. Create a key in the "
                + "OmniSocials app under Settings -> API Keys.",
            null);
      }
      return new OmniSocials(this, key);
    }
  }
}
