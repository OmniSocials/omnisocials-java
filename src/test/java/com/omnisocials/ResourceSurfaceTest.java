package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omnisocials.resources.AccountsResource;
import com.omnisocials.resources.AdAccountsResource;
import com.omnisocials.resources.AdAudiencesResource;
import com.omnisocials.resources.AdBudgetSchedulesResource;
import com.omnisocials.resources.AdCampaignAdsResource;
import com.omnisocials.resources.AdCommentsResource;
import com.omnisocials.resources.AdConversionsResource;
import com.omnisocials.resources.AdDraftsResource;
import com.omnisocials.resources.AdGoogleResource;
import com.omnisocials.resources.AdInsightsResource;
import com.omnisocials.resources.AdLeadFormsResource;
import com.omnisocials.resources.AdLeadsResource;
import com.omnisocials.resources.AdMediaResource;
import com.omnisocials.resources.AdObjectsResource;
import com.omnisocials.resources.AdPartnershipResource;
import com.omnisocials.resources.AdPreviewsResource;
import com.omnisocials.resources.AdReachFrequencyResource;
import com.omnisocials.resources.AdReportsResource;
import com.omnisocials.resources.AdResultsResource;
import com.omnisocials.resources.AdRulesResource;
import com.omnisocials.resources.AdStudiesResource;
import com.omnisocials.resources.AdTargetingResource;
import com.omnisocials.resources.AdsResource;
import com.omnisocials.resources.AnalyticsResource;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Every resource method from the SDK design spec exists with the expected name. */
class ResourceSurfaceTest {

  private static void assertHasMethods(Class<?> cls, String... names) {
    Set<String> publicMethods =
        Arrays.stream(cls.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toSet());
    for (String name : names) {
      assertTrue(
          publicMethods.contains(name),
          cls.getSimpleName() + " is missing method `" + name + "` (has: " + publicMethods + ")");
    }
  }

  @Test
  void postsSurface() {
    assertHasMethods(
        PostsResource.class,
        "list", "get", "create", "createAndPublish", "update", "delete", "publish",
        "recentPlatform", "approve", "reject", "getApproval");
  }

  @Test
  void mediaSurface() {
    assertHasMethods(
        MediaResource.class,
        "list", "get", "upload", "uploadFromUrl", "uploadFromBase64", "createUploadUrl", "check",
        "update", "delete");
  }

  @Test
  void foldersSurface() {
    assertHasMethods(FoldersResource.class, "list", "create", "update", "delete");
  }

  @Test
  void hashtagSetsSurface() {
    assertHasMethods(
        HashtagSetsResource.class, "list", "get", "create", "update", "delete");
  }

  @Test
  void tagsSurface() {
    assertHasMethods(TagsResource.class, "list", "create", "update", "delete", "apply");
  }

  @Test
  void accountsSurface() {
    assertHasMethods(AccountsResource.class, "list", "get");
  }

  @Test
  void analyticsSurface() {
    assertHasMethods(
        AnalyticsResource.class, "post", "posts", "overview", "accounts", "bestTimes", "tags");
  }

  @Test
  void locationsSurface() {
    assertHasMethods(LocationsResource.class, "search", "validate");
  }

  @Test
  void pinterestSurface() {
    assertHasMethods(PinterestResource.class, "listProducts", "validateProduct");
  }

  @Test
  void automationsSurface() {
    assertHasMethods(
        AutomationsResource.class, "list", "get", "create", "update", "delete", "listEvents");
  }

  @Test
  void adsSurface() {
    assertHasMethods(
        AdsResource.class,
        "drafts", "listAccounts", "capabilities", "list", "get", "boost", "boostMulti",
        "update", "activate", "activateMulti", "pause", "resume", "delete", "overview",
        "suggestions", "researchKeywords", "keywordMetrics");
    // POST /ads/campaigns was removed: a new ad is ads().drafts().create().
    assertTrue(
        Arrays.stream(AdsResource.class.getDeclaredMethods())
            .noneMatch(m -> m.getName().equals("create")),
        "AdsResource still has create");
    assertHasMethods(
        AdDraftsResource.class,
        "defaults", "create", "get", "update", "delete", "attachAccount", "launch");
  }

  @Test
  void adsParitySurface() {
    assertHasMethods(
        AdsResource.class,
        "objects", "campaignAds", "comments", "targeting", "audiences", "leadForms", "leads",
        "conversions", "insights", "reports", "rules", "results", "media", "previews",
        "partnership", "accounts", "reachFrequency", "studies", "budgetSchedules", "google",
        "bulkStatus", "duplicate");
    assertHasMethods(AdCampaignAdsResource.class, "list", "add", "get", "setStatus", "review");
    assertHasMethods(
        AdObjectsResource.class,
        "tree", "get", "update", "setStatus", "duplicate", "importStatus", "startImport",
        "listExternal", "getExternal");
    assertHasMethods(AdTargetingResource.class, "search", "browse", "reachEstimate", "bidSuggestions");
    assertHasMethods(
        AdAudiencesResource.class,
        "list", "create", "get", "update", "delete", "addMembers", "removeMembers", "addCompanies");
    assertHasMethods(
        AdLeadFormsResource.class, "list", "create", "get", "update", "delete", "sendTestLead");
    assertHasMethods(AdLeadsResource.class, "list", "counts", "update");
    assertHasMethods(AdConversionsResource.class, "destinations", "goals", "createCustom", "sendEvents");
    assertHasMethods(
        AdInsightsResource.class, "query", "campaign", "ad", "timeline", "performance", "alerts");
    assertHasMethods(AdReportsResource.class, "list", "start", "get");
    assertHasMethods(
        AdRulesResource.class,
        "list", "create", "templates", "preview", "activity", "undo", "get", "update", "delete",
        "previewSaved", "runs");
    assertHasMethods(AdResultsResource.class, "list", "upload", "totals", "metrics", "putMetric");
    assertHasMethods(AdMediaResource.class, "list", "upload", "get", "delete");
    assertHasMethods(AdPreviewsResource.class, "create", "forAd");
    assertHasMethods(AdPartnershipResource.class, "listContent", "listPermissions", "setPermission");
    assertHasMethods(AdCommentsResource.class, "list", "reply", "hide", "delete");
    assertHasMethods(
        AdAccountsResource.class,
        "health", "finance", "updateSettings", "listUsers", "addUser", "removeUser", "activity",
        "businesses", "dsa", "hierarchy", "identities");
    assertHasMethods(AdReachFrequencyResource.class, "create", "get", "cancel", "reserve");
    assertHasMethods(AdStudiesResource.class, "list", "create");
    assertHasMethods(AdBudgetSchedulesResource.class, "list", "create", "delete");
    assertHasMethods(
        AdGoogleResource.class,
        "searchTerms", "recommendations", "keywords", "assets", "sharedBudgets", "bidStrategies",
        "adSchedule", "labels", "localServicesLeads");
  }

  @Test
  void inboxSurface() {
    assertHasMethods(
        InboxResource.class,
        "listConversations", "getMessages", "markRead", "reply", "hide", "like", "deleteMessage",
        "next");
  }

  @Test
  void webhooksSurface() {
    assertHasMethods(
        WebhooksResource.class, "list", "get", "create", "update", "delete", "rotateSecret");
  }

  @Test
  void limitsSurface() {
    assertHasMethods(LimitsResource.class, "list");
  }

  @Test
  void featureRequestsSurface() {
    assertHasMethods(FeatureRequestsResource.class, "create", "list", "get");
  }

  @Test
  void clientExposesAllResourcesAndHealth() {
    OmniSocials client = OmniSocials.builder().apiKey("omsk_test_key").build();
    assertNotNull(client.posts());
    assertNotNull(client.media());
    assertNotNull(client.folders());
    assertNotNull(client.hashtagSets());
    assertNotNull(client.tags());
    assertNotNull(client.accounts());
    assertNotNull(client.analytics());
    assertNotNull(client.locations());
    assertNotNull(client.pinterest());
    assertNotNull(client.automations());
    assertNotNull(client.ads());
    assertNotNull(client.ads().drafts());
    assertNotNull(client.inbox());
    assertNotNull(client.webhooks());
    assertNotNull(client.limits());
    assertNotNull(client.featureRequests());
    assertHasMethods(OmniSocials.class, "health", "fromEnv", "builder");
  }

  @Test
  void paramsBuilderWorks() {
    var params =
        Params.builder()
            .put("content", "Hello")
            .put("channels", java.util.List.of("instagram"))
            .put("thread_parts", null)
            .build();
    assertTrue(params.containsKey("thread_parts"), "null values must be kept for JSON null");
    assertTrue(params.get("content").equals("Hello"));

    var pairs = Params.of("platform", "instagram", "timezone", "Europe/Amsterdam");
    assertTrue(pairs.get("platform").equals("instagram"));

    // Nested explicit null survives inside a platform options map: this is how
    // an update clears a Threads chain (same for x / bluesky / mastodon).
    var threadsClear = Params.builder().put("thread_parts", null).build();
    var update = Params.builder().put("threads", threadsClear).build();
    @SuppressWarnings("unchecked")
    var threadsMap = (java.util.Map<String, Object>) update.get("threads");
    assertTrue(threadsMap.containsKey("thread_parts"), "nested null thread_parts must be kept");
    assertTrue(threadsMap.get("thread_parts") == null);
  }
}
