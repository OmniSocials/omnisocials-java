package com.omnisocials;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omnisocials.resources.AccountsResource;
import com.omnisocials.resources.AnalyticsResource;
import com.omnisocials.resources.FoldersResource;
import com.omnisocials.resources.InboxResource;
import com.omnisocials.resources.LocationsResource;
import com.omnisocials.resources.MediaResource;
import com.omnisocials.resources.PostsResource;
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
        "recentPlatform");
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
  void accountsSurface() {
    assertHasMethods(AccountsResource.class, "list", "get");
  }

  @Test
  void analyticsSurface() {
    assertHasMethods(
        AnalyticsResource.class, "post", "posts", "overview", "accounts", "bestTimes");
  }

  @Test
  void locationsSurface() {
    assertHasMethods(LocationsResource.class, "search", "validate");
  }

  @Test
  void inboxSurface() {
    assertHasMethods(
        InboxResource.class, "listConversations", "getMessages", "markRead", "reply");
  }

  @Test
  void webhooksSurface() {
    assertHasMethods(
        WebhooksResource.class, "list", "get", "create", "update", "delete", "rotateSecret");
  }

  @Test
  void clientExposesAllResourcesAndHealth() {
    OmniSocials client = OmniSocials.builder().apiKey("omsk_test_key").build();
    assertNotNull(client.posts());
    assertNotNull(client.media());
    assertNotNull(client.folders());
    assertNotNull(client.accounts());
    assertNotNull(client.analytics());
    assertNotNull(client.locations());
    assertNotNull(client.inbox());
    assertNotNull(client.webhooks());
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
  }
}
