package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/** Posts: drafts, scheduled, and published. Accessed via {@code client.posts()}. */
public final class PostsResource extends ApiResource {

  public PostsResource(OmniSocials client) {
    super(client);
  }

  /** {@code GET /posts} - list posts in the workspace (newest first). */
  public JsonNode list() {
    return client.get("/posts");
  }

  /**
   * {@code GET /posts?status=&limit=&offset=&start_date=&end_date=} - list
   * posts with filters. Query params: {@code status} (draft | in_approval |
   * scheduled | posting | published | failed | warning; in_approval = waiting
   * for a reviewer in an approval workflow), {@code limit}, {@code offset},
   * {@code start_date} and {@code end_date} (posts whose {@code schedule_at}
   * falls in the period, both inclusive: "YYYY-MM-DD" a UTC day, "YYYY-MM" a
   * month, or an ISO 8601 datetime). With a range the posts come in calendar
   * order (earliest first). {@code tag}: only posts with this tag (a tag id or
   * name, comma separated for any of several; an unknown tag is 404
   * {@code tag_not_found}).
   */
  public JsonNode list(Map<String, Object> query) {
    return client.get("/posts", query);
  }

  /** {@code GET /posts/:id} - fetch a single post. */
  public JsonNode get(String id) {
    return client.get("/posts/" + seg(id));
  }

  /**
   * {@code GET /posts/recent-platform} - recent posts fetched live from the
   * connected platform APIs (including content published outside
   * OmniSocials). The fallback for brand-new workspaces where {@code list()}
   * is empty. Requires the {@code analytics:read} scope.
   */
  public JsonNode recentPlatform() {
    return client.get("/posts/recent-platform");
  }

  /**
   * {@code GET /posts/recent-platform?limit=&platforms=} - query params:
   * {@code limit}, {@code platforms} (a {@code List} or comma-separated string).
   */
  public JsonNode recentPlatform(Map<String, Object> query) {
    return client.get("/posts/recent-platform", query);
  }

  /**
   * {@code POST /posts/create} - create a draft or scheduled post.
   *
   * <p>Params: {@code content} (string, or a map of per-platform captions with
   * a {@code default} key), {@code channels} (list of platform ids),
   * {@code scheduled_at} (ISO 8601; omit for a draft), {@code media_urls},
   * {@code media_ids}, {@code location_id}, plus per-platform option maps
   * ({@code instagram}, {@code youtube}, {@code x} with {@code thread_parts},
   * {@code bluesky}, {@code mastodon}, {@code threads}, ...). The same
   * {@code thread_parts} shape (2 to 25 parts) publishes a chained thread on
   * X, Bluesky, Mastodon and Threads. Threads: 500 characters per part, up to
   * 10 media per part; parts after the first publish as replies to the
   * previous part, and the Threads caption is taken from part 1.
   *
   * <p>Threads posts can carry a location tag: {@code location_id} inside the
   * {@code threads} map (an id from {@code GET
   * /locations/search?platform=threads}), or {@code location} ({@code { id,
   * name?, address?, city?, country? }}) to store display fields along with
   * the id; {@code location_id} wins when both are given. On a multi-post
   * thread the tag is applied to part 1, and the Post's {@code threads} block
   * echoes a {@code location} object when set. Threads location tagging is
   * currently rolling out; until Meta approves the permissions it is disabled
   * on production and create/update/publish return a 400 (also a 400
   * {@code validation_error} asking you to reconnect Threads when the
   * connection lacks the {@code threads_location_tagging} permission).
   *
   * <p>Threads posts can carry a topic: {@code topic_tag} inside the
   * {@code threads} map, for example "Build in public" (1 to 50 characters,
   * no period and no ampersand; a topic can hold spaces). On a multi-post
   * thread it goes on part 1; on update an explicit {@code null} removes it.
   *
   * <p>Pinterest posts can tag products: {@code product_tags} inside the
   * {@code pinterest} map takes up to 24 product Pins of the connected
   * Pinterest account, each as a Pin id string (see
   * {@link PinterestResource#listProducts()}) or a Pin link. Products of
   * other merchants cannot be tagged. The tags are added right after the Pin
   * is published; a product Pinterest refuses never fails the post, and the
   * Post's {@code pinterest} block then carries {@code product_tags_result}
   * ({@code requested}, {@code tagged}, {@code skipped}, {@code error}). More
   * than 24 entries or an invalid entry throws a 400
   * {@code validation_error}.
   *
   * <p>Telegram posts take a {@code telegram} map (the connected channel or
   * group, posted by the OmniSocials bot): {@code silent}, {@code
   * link_preview}, {@code pin}, {@code protect_content}, {@code spoiler},
   * {@code caption_above_media} (booleans), up to 3 {@code buttons}
   * ({@code { text, url }}, not on an album or a poll), a native {@code poll}
   * ({@code { options: 2 to 12 of up to 100 characters, open_period: 5 to
   * 2628000 seconds, multiple }}; the post text is the question, max 300
   * characters, no media) and {@code first_comment} (a reply under the
   * post). Text up to 4,096 characters; with media, text over 1,024
   * characters is sent as a reply under the media. No Stories. On update
   * every key merges into the stored options. Once sent, the Post's
   * {@code telegram} block carries {@code result} (message ids and links).
   *
   * <p>Discord posts take a {@code discord} map (one Discord channel, posted
   * through its incoming webhook with the webhook's name and picture):
   * {@code silent}, {@code link_preview} (default on), {@code
   * allow_mentions} (default off: @everyone, @here, role and user mentions
   * show as text and ping nobody), {@code spoiler} (booleans), {@code
   * thread_name} (the post title in a forum or media channel, 1 to 100
   * characters; on a text channel the post goes out without it), up to 5
   * {@code buttons}
   * ({@code { text: 1 to 80 characters, url: https:// or http:// }}, one
   * row, not with a poll) and a native {@code poll} ({@code { options: 2 to
   * 10 of up to 55 characters, duration_hours: 1 to 768 (default 24),
   * multiple }}; the post text is the question, max 300 characters, no
   * media and no buttons). Text up to 2,000 characters; up to 10 images,
   * GIFs and videos per message, a video or GIF over 20 MB is sent as a link
   * in the text. No Stories. Once sent, the Post's {@code discord} block
   * carries {@code result} (message id and link).
   *
   * <p>Each {@code media_urls} / {@code media_ids} entry is a plain string, or
   * a map with an {@code alt} accessibility description (max 1500 chars):
   * {@code Map.of("url", "https://...", "alt", "...")} for media_urls,
   * {@code Map.of("id", "...", "alt", "...")} for media_ids. Alt text is
   * delivered to Mastodon (media description), Bluesky (embed alt), X
   * (photos/GIFs), Pinterest (pin alt text), Instagram (images), and
   * LinkedIn (images); the same entry shape works inside
   * {@code thread_parts} media.
   *
   * <p>When the post targets X and its text (or any thread part) contains a
   * URL, the response includes a top-level {@code warnings} array (sibling of
   * {@code data}) with a {@code x_url_post_credits} entry carrying
   * {@code credits_required} and {@code credits_balance}: X's link-post fee
   * is passed through as prepaid credits, debited at publish time (from
   * 2026-08-14). Credits are managed in the dashboard, not the API.
   *
   * <p>From 2026-08-14, scheduling an X link post can also be refused up
   * front, before the request is accepted: every scheduled X link post
   * reserves its cost, and if reserving this one would push the company's
   * total reserved credits past its balance, this call throws an
   * {@link com.omnisocials.errors.ApiException} with status 402 and code
   * {@code x_credits_insufficient}, whose body carries
   * {@code error.details.credits_required}, {@code credits_balance}, and
   * {@code credits_reserved}. Drafts (no {@code scheduled_at}) are never
   * gated, and posts scheduled to publish before 2026-08-14 are never gated.
   * The same gate applies to {@link #update(String, Map)} and
   * {@link #publish(String)}.
   *
   * <p>{@code approval_workflow_id} (a workflow id from
   * {@link ApprovalWorkflowsResource#list()}) routes the post through a
   * saved approval workflow: it is created as {@code in_approval}
   * ({@code approval_status: "pending"}) instead of {@code scheduled}, the
   * approvers are notified, and it publishes at {@code scheduled_at} once the
   * last step approves. Requires {@code scheduled_at}; not allowed with
   * {@code publish_now}. Errors: {@code 404 workflow_not_found},
   * {@code 400 validation_error}.
   *
   * <p>{@code draft: true} with {@code scheduled_at} stores a planned draft:
   * the post is {@code draft} and keeps that time, shows on the dashboard
   * calendar at that slot with a dashed outline, and the scheduler never
   * publishes it. A person schedules it from the composer, or you do with
   * {@link #update(String, Map)} and {@code draft: false}. Use it when an
   * agent proposes a slot that a human should confirm. Not allowed with
   * {@code publish_now} or {@code approval_workflow_id}
   * ({@code 400 validation_error}). Without {@code scheduled_at} it changes
   * nothing.
   *
   * <p>{@code media_fit} is the image fit per channel for feed images whose
   * ratio is outside what a channel shows uncropped: a map of platform id
   * ({@code "instagram"}, {@code "pinterest"}, ...) to {@code "auto"},
   * {@code "crop"} or {@code "keep"}. {@code auto} (default): Instagram crops
   * a tall or wide image to 4:5 / 1.91:1 around the subject (Instagram cuts
   * it to that window anyway and Meta cannot boost a feed post outside it);
   * Pinterest keeps a Pin taller than 2:3 and reports it. {@code crop} /
   * {@code keep} force it per channel. Every decision comes back in the same
   * {@code warnings} array with code {@code image_fit} ({@code action} is
   * {@code cropped}, {@code kept} or {@code crop_failed}, with
   * {@code platform}, {@code media_url}, {@code ratio}, {@code target} and
   * {@code crop_url}).
   *
   * <p>{@code comment_dm_automation} binds a comment-to-DM automation to the
   * post (Instagram or Facebook must be in {@code channels}): a keyword in a
   * comment is answered with a DM, once per person, with an optional public
   * reply under the comment. A map of {@code trigger} ({@code keywords},
   * {@code match}), {@code actions} ({@code dm} with {@code text}, optional
   * {@code public_reply} with {@code enabled} and {@code variations},
   * optional {@code follow_gate}, Follow first, which applies to the
   * Instagram automation only), {@code limits} ({@code max_sends},
   * {@code stop_at}) and
   * {@code platforms} ({@code ["instagram", "facebook"]}: one automation per
   * listed channel, each a channel of the post; without it Instagram when the
   * post goes there, else Facebook); see
   * {@link AutomationsResource#create(Map)} for the fields. Needs the
   * {@code automations:write} scope on the key ({@code 403
   * insufficient_scope}, checked before anything else); a bad map is a
   * {@code 400 validation_error} whose message starts with
   * {@code comment_dm_automation: }. The response then carries
   * {@code data.comment_dm_automation} (the first automation, with
   * {@code post} null, plus {@code platforms} and {@code automations}, one
   * {@code id} / {@code platform} / {@code status} per channel); each starts
   * listening once the post is live on its channel.
   *
   * <p>{@code tags}: a list of tag ids or names (at most 20); a name that
   * does not exist yet becomes a tag of the workspace. Tags are the
   * workspace's own labels to filter and compare posts, never sent to a
   * platform; see {@link TagsResource}. The post carries {@code tags} (the
   * tag objects) and {@code trial_reel} ({@code { strategy }} for an
   * Instagram Trial Reel, else {@code null}).
   *
   * <p>A media id that is still processing (a large video is checked and
   * converted after upload) answers {@code 409 media_processing} and nothing
   * is created: wait until {@code media().get(id)} says {@code ready}, then
   * send the same request again. A file whose processing failed answers
   * {@code 400 invalid_media}: upload it again.
   */
  public JsonNode create(Map<String, Object> params) {
    return client.post("/posts/create", params);
  }

  /**
   * {@code POST /posts/create-and-publish} - create a post and publish it immediately.
   * See {@link #create(Map)} for the {@code warnings} array (X link posts,
   * and the {@code image_fit} entries steered by {@code media_fit}), the
   * X link-post credit gate (402 {@code x_credits_insufficient}) and the
   * {@code 409 media_processing} / {@code 400 invalid_media} answers.
   */
  public JsonNode createAndPublish(Map<String, Object> params) {
    return client.post("/posts/create-and-publish", params);
  }

  /**
   * {@code PATCH /posts/:id} - update a draft or scheduled post. Pass
   * {@code thread_parts: null} inside a platform options map ({@code x},
   * {@code bluesky}, {@code mastodon}, {@code threads}) to clear thread
   * mode; omit it to leave the existing thread untouched. A Threads location
   * tag clears the same way: {@code location_id: null} (or
   * {@code location: null}) inside {@code threads} removes the tag, while
   * omitting the key leaves it untouched.
   *
   * <p>The {@code pinterest} map replaces the stored Pinterest options
   * wholesale, so leave {@code product_tags} out (or send an empty list) to
   * remove the product tags.
   *
   * <p>{@code draft: false} without {@code scheduled_at} schedules a planned
   * draft at the {@code scheduled_at} it already carries
   * ({@code 400 validation_error} when it has none or it is in the past).
   * {@code draft: true} without {@code scheduled_at} unschedules a scheduled
   * post: it keeps its time and stays on the calendar as a dashed draft,
   * nothing publishes; a post waiting for approval leaves its workflow.
   * {@code draft: true} with {@code scheduled_at} moves the time and keeps
   * (or makes) the post a draft.
   *
   * <p>{@code media_fit} (same shape as on {@link #create(Map)}) alone
   * re-decides the stored images: {@code "keep"} gives the original back,
   * {@code "crop"} crops an image that was kept. Like {@link #create(Map)},
   * the response can carry a top-level {@code warnings} array with
   * {@code image_fit} entries when the update changes the images or
   * {@code media_fit}.
   *
   * <p>See {@link #create(Map)} for the X link-post credit gate (402
   * {@code x_credits_insufficient}), which also applies here when the update
   * (re)schedules an X post whose text contains a URL.
   *
   * <p>{@code comment_dm_automation} (same shape as on {@link #create(Map)})
   * as a map replaces the post's comment-to-DM automations (a channel no
   * longer in {@code platforms} loses its automation), an explicit
   * {@code null} ({@code Params.builder().put("comment_dm_automation", null)})
   * removes them all, and omitting the key leaves them untouched. Needs the
   * {@code automations:write} scope.
   *
   * <p>{@code discord} merges into the stored Discord options: a key set to
   * {@code null} ({@code Params.of("poll", null)}) removes that option, an
   * explicit {@code null} for {@code discord} removes every option, and
   * omitting the key leaves them untouched.
   *
   * <p>{@code tags} (tag ids or names) replaces the post's tags; an explicit
   * {@code null} or an empty list removes them all. An update that sends
   * only {@code tags} works on every post, published ones included. To
   * change a few tags on many posts at once, use
   * {@link TagsResource#apply(Map)}.
   *
   * <p>409 answers, where nothing is changed: {@code post_publishing} while
   * the post is publishing (status {@code posting}) or a retry publishes one
   * of its channels (poll {@code get(id)} until {@code published},
   * {@code warning} or {@code failed}; an update with only {@code tags}
   * still works); {@code review_required} when changes were requested on
   * the post ({@code approval_status} {@code rejected}) and the update
   * would schedule it (it needs a new review: create it again with
   * {@code approval_workflow_id}; saving it as a draft works);
   * {@code media_processing} as on {@link #create(Map)}.
   */
  public JsonNode update(String id, Map<String, Object> params) {
    return client.patch("/posts/" + seg(id), params);
  }

  /**
   * {@code DELETE /posts/:id} - remove a post from OmniSocials (the live
   * post stays on the platform). Returns {@code null} (204). While the post
   * is publishing (status {@code posting}) or a retry publishes one of its
   * channels, the answer is {@code 409 post_publishing} and nothing is
   * deleted: poll {@code get(id)} until {@code published}, {@code warning}
   * or {@code failed}, then delete it.
   */
  public JsonNode delete(String id) {
    return client.delete("/posts/" + seg(id));
  }

  /**
   * {@code POST /posts/:id/publish} - publish a draft or scheduled post now.
   *
   * <p>See {@link #create(Map)} for the X link-post credit gate (402
   * {@code x_credits_insufficient}), which also applies here. A post on
   * which changes were requested in its approval workflow
   * ({@code approval_status} {@code rejected}) answers
   * {@code 409 review_required}: it publishes only after a new review
   * (create it again with {@code approval_workflow_id}).
   */
  public JsonNode publish(String id) {
    return client.post("/posts/" + seg(id) + "/publish");
  }

  /**
   * {@code POST /posts/:id/retry} - retry the failed platforms of a
   * {@code failed} or {@code warning} (partially failed) post, on the same
   * post. Only the platforms that failed are re-published; platforms that
   * already succeeded are never posted again. Asynchronous: a 200 means the
   * retry is queued - poll {@code get(id)} for the outcome. Max 3 retries per
   * platform. While another retry runs (a second request, the dashboard,
   * the automatic retry) the answer is {@code 409 retry_in_progress} and
   * nothing is queued: poll {@code get(id)} and retry once the post is
   * {@code failed} or {@code warning} again.
   */
  public JsonNode retry(String id) {
    return client.post("/posts/" + seg(id) + "/retry");
  }

  /**
   * {@code POST /posts/:id/approve} - approve the current step of a post's
   * approval workflow, on behalf of the user who owns this API key. That
   * user must be a listed approver for the workflow's CURRENT step: steps
   * approve in order, so an approver on a later step gets a 403 {@code
   * forbidden} error until earlier steps clear. Only works on a post with
   * {@code approval_status: "pending"}. If this is the last step, the post
   * finalizes immediately ({@code scheduled} or {@code posting});
   * otherwise it stays {@code in_approval} and the next step's approvers
   * are notified. Without a step this approves whatever step is current;
   * prefer {@link #approve(String, Integer)}.
   */
  public JsonNode approve(String id) {
    return approve(id, null);
  }

  /**
   * {@code POST /posts/:id/approve} with {@code step_order}: the {@code
   * current_step} of {@link #getApproval(String)}, the step you reviewed
   * ({@code null} leaves it out). When the review is on another step now
   * (somebody decided since you read it, or a retried call already went
   * through), nothing is approved and the call throws a 409 {@link
   * com.omnisocials.errors.ApiException} with code {@code step_changed};
   * {@code getBody().path("error").path("current_step_order")} is the step
   * the review is on now. Read the approval again before you decide.
   * Without a step, a retry after a timeout can approve the next step too.
   */
  public JsonNode approve(String id, Integer stepOrder) {
    if (stepOrder == null) {
      return client.post("/posts/" + seg(id) + "/approve");
    }
    return client.post("/posts/" + seg(id) + "/approve", java.util.Map.of("step_order", stepOrder));
  }

  /**
   * {@code POST /posts/:id/reject} - reject a post's approval workflow, on
   * behalf of the user who owns this API key. Same approver requirement as
   * {@link #approve(String)}. Unlike approval, a rejection stops the WHOLE
   * workflow immediately (not just the current step): the post's status
   * becomes {@code rejected}. {@code comment} is optional (pass null; max
   * 2000 characters) and, when given, is shown to the requester and other
   * approvers in the post's review thread.
   */
  public JsonNode reject(String id, String comment) {
    return reject(id, comment, null);
  }

  /**
   * {@code POST /posts/:id/reject} with a comment and {@code step_order}
   * (both optional, pass null). {@code stepOrder} works as on {@link
   * #approve(String, Integer)}: when the review moved on, nothing is
   * rejected and the call throws a 409 {@code step_changed} {@link
   * com.omnisocials.errors.ApiException} with {@code current_step_order} in
   * its body.
   */
  public JsonNode reject(String id, String comment, Integer stepOrder) {
    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
    if (comment != null) body.put("comment", comment);
    if (stepOrder != null) body.put("step_order", stepOrder);
    if (body.isEmpty()) {
      return client.post("/posts/" + seg(id) + "/reject");
    }
    return client.post("/posts/" + seg(id) + "/reject", body);
  }

  /** {@code POST /posts/:id/reject} with no comment. See {@link #reject(String, String)}. */
  public JsonNode reject(String id) {
    return reject(id, null, null);
  }

  /**
   * {@code GET /posts/:id/approval} - the approval review of a post: every
   * step with its approvers and their decisions, the rejection with its
   * reason, and the comment thread. Use it when {@code approval_status} is
   * {@code rejected} to learn who rejected the post and why, or while it is
   * {@code pending} to see who the post waits for.
   *
   * <p>{@code data} carries {@code post_id}, {@code status} ({@code none},
   * {@code pending}, {@code approved}, {@code rejected}), {@code workflow},
   * {@code requested_by}, {@code requested_at}, {@code current_step},
   * {@code steps}, {@code rejection} and {@code comments} (oldest first). A
   * post without an approval workflow returns {@code status: "none"} with
   * the object fields null and empty {@code steps} and {@code comments}.
   * Read-only; requires the {@code posts:read} scope.
   */
  public JsonNode getApproval(String id) {
    return client.get("/posts/" + seg(id) + "/approval");
  }
}
