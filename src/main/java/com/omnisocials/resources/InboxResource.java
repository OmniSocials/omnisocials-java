package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Social inbox: DMs, comments, and mentions across connected platforms
 * (Instagram, Facebook, LinkedIn, TikTok comments, YouTube comments, X DMs,
 * and Threads comments and mentions). Accessed via {@code client.inbox()}.
 *
 * <p>Threads conversations are {@code type} {@code comment} (replies people
 * leave on the user's Threads posts; conversation ids look like
 * {@code threads_comment_<rootPostId>}) and {@code mention}
 * ({@code threads_mention_<postId>}); there are no Threads DMs. Threads inbox
 * is currently rolling out: until Meta approves the permissions it is
 * disabled on production and calls return a clear error, and it needs a
 * Threads connection with the reply permission.
 *
 * <p>Unlike the offset-paginated list endpoints elsewhere in the API, the inbox
 * list endpoints use <b>cursor pagination</b>. The {@code pagination} object is
 * {@code { next_cursor: String|null, has_more: boolean, limit: int }}. To page
 * on, pass the previous response's {@code pagination.next_cursor} back as the
 * request's {@code cursor} while {@code pagination.has_more} is {@code true};
 * {@code next_cursor} is {@code null} on the last page.
 *
 * <p>Methods return the parsed response body as a {@link JsonNode}. The shapes:
 *
 * <ul>
 *   <li><b>InboxConversation</b> ({@code conversation_id}, {@code platform},
 *       {@code type} = {@code dm|comment|mention}, {@code participant},
 *       {@code unread_count}, {@code last_message} (with {@code id},
 *       {@code direction} = {@code incoming|outgoing}, {@code text},
 *       {@code timestamp}, {@code is_read}), {@code post} (null for DMs)).
 *   <li><b>InboxMessage</b> ({@code id}, {@code conversation_id},
 *       {@code platform}, {@code type}, {@code direction}, {@code text},
 *       {@code timestamp}, {@code is_read}, {@code is_replied},
 *       {@code reaction}, {@code parent_comment_id}, {@code sender},
 *       {@code post}, {@code hidden}, {@code permalink}). {@code hidden} is
 *       set on comments and mentions: {@code true} when the comment is
 *       hidden on the platform (see {@link #hide(String, boolean)}),
 *       {@code false} when it is not; JSON {@code null} for DMs.
 *       {@code permalink} links to the reply or mentioning post on the
 *       platform, when known ({@code null} otherwise).
 *   <li><b>InboxParticipant</b> ({@code id}, {@code name}, {@code username},
 *       {@code profile_picture}) - the person on the other side of a
 *       conversation, or a message's {@code sender}.
 *   <li><b>InboxPostRef</b> ({@code id}, {@code caption}, {@code thumbnail},
 *       {@code url}, {@code media_type}) - the post a comment/mention is
 *       attached to; {@code null} for DMs. {@code url} is the public link to
 *       the post when the platform provides one, {@code media_type} the
 *       platform's own label (e.g. {@code IMAGE}, {@code VIDEO},
 *       {@code CAROUSEL_ALBUM} on Instagram) when known; both {@code null}
 *       otherwise.
 *   <li><b>InboxNextUnanswered</b> ({@code conversation}, {@code message},
 *       {@code messages}) - what {@link #next(Map)} returns under
 *       {@code data} (JSON {@code null} when nothing is waiting), with
 *       {@code remaining} beside it.
 * </ul>
 */
public final class InboxResource extends ApiResource {

  public InboxResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /inbox/conversations} - list social inbox conversations (DMs,
   * comments, and mentions) across connected platforms, newest activity first.
   */
  public JsonNode listConversations() {
    return client.get("/inbox/conversations");
  }

  /**
   * {@code GET /inbox/conversations?platform=&type=&unread=&limit=&cursor=} -
   * list conversations with filters. Query params (all optional):
   * {@code platform} ({@code instagram} | {@code facebook} | {@code linkedin} |
   * {@code tiktok} | {@code youtube} | {@code x} | {@code threads}),
   * {@code type} ({@code dm} | {@code comment} | {@code mention}),
   * {@code unread} (boolean), {@code unanswered} (boolean: only conversations
   * that still need an answer - the customer's latest DM has no reply after
   * it, for Instagram/Facebook DMs within the 24-hour messaging window only,
   * or a comment/mention that has not been replied to and is not hidden;
   * replies typed in the native apps count as answers, and read state is
   * ignored, so use {@link #next(Map)} for a work queue), {@code limit}
   * (1-100), {@code cursor} (an opaque cursor from a previous response's
   * {@code pagination.next_cursor}).
   */
  public JsonNode listConversations(Map<String, Object> query) {
    return client.get("/inbox/conversations", query);
  }

  /**
   * {@code GET /inbox/conversations/:conversationId/messages} - fetch the full
   * message history for a single conversation, newest first.
   *
   * <p>{@code conversationId} is URL-encoded for you, so pass it exactly as
   * returned - LinkedIn conversation ids contain {@code :} and {@code ()}
   * (e.g. {@code linkedin_comment_urn:li:activity:123}).
   */
  public JsonNode getMessages(String conversationId) {
    return client.get("/inbox/conversations/" + seg(conversationId) + "/messages");
  }

  /**
   * {@code GET /inbox/conversations/:conversationId/messages?limit=&cursor=} -
   * cursor-paginated message history (same {@code { next_cursor, has_more,
   * limit }} pagination shape as {@link #listConversations()}). Query params
   * (both optional): {@code limit}, {@code cursor}.
   *
   * <p>{@code conversationId} is URL-encoded for you.
   */
  public JsonNode getMessages(String conversationId, Map<String, Object> query) {
    return client.get("/inbox/conversations/" + seg(conversationId) + "/messages", query);
  }

  /**
   * {@code POST /inbox/conversations/:conversationId/read} - mark every message
   * in the conversation as read. No request body. Returns
   * {@code { conversation_id, marked_read }}, where {@code marked_read} is the
   * number of messages that were newly marked read.
   *
   * <p>{@code conversationId} is URL-encoded for you.
   */
  public JsonNode markRead(String conversationId) {
    return client.post("/inbox/conversations/" + seg(conversationId) + "/read");
  }

  /**
   * {@code POST /inbox/conversations/:conversationId/reply} - send a reply into
   * the conversation (a DM message, or a reply to the comment/mention). Returns
   * the created outbound message as {@code { data: InboxMessage }}.
   *
   * <p>Params: {@code text} (string; optional when {@code attachment_url} is
   * set - an attachment-only reply is allowed), {@code attachment_url}
   * (optional public URL of a single media asset to attach, Facebook and
   * Instagram DMs only), {@code attachment_type} (optional; {@code image} |
   * {@code video} | {@code audio} | {@code file}, pair with
   * {@code attachment_url}). Other platforms are text-only and require
   * {@code text}. The returned message's {@code attachment} field carries
   * the same shape when the message has media.
   *
   * <p>On a Threads conversation the reply publishes as a native Threads
   * reply. Threads inbox is currently rolling out (disabled on production
   * until Meta App Review) and needs a Threads connection with the reply
   * permission: a 401 with code {@code reauth_required} means the connection
   * lacks that permission (reconnect Threads).
   *
   * <p>X DM replies cost 2 prepaid credits per send, debited from the company
   * balance before the message is sent and automatically refunded if the send
   * fails. This can throw an {@link com.omnisocials.errors.ApiException} with
   * status 402 and code {@code insufficient_credits} (the balance can't cover
   * the 2 credits) or {@code x_inbox_suspended} (the workspace's X inbox was
   * auto-suspended after hitting a zero balance; top up and re-enable it in
   * the dashboard to resume - DMs that arrived while suspended are not
   * recovered). Replies on other platforms are free. TikTok replies are
   * comments only, text-only, and capped at 150 characters. YouTube replies
   * are comments only (YouTube has no DMs).
   *
   * <p>Pass {@code include_next} = {@code true} to also get {@code next} (the
   * next conversation that needs an answer, the same object
   * {@link #next(Map)} returns under {@code data}, using its default queue
   * order and filters; JSON {@code null} when nothing is waiting) and
   * {@code remaining} in the response. Saves the extra call when working
   * through the inbox.
   *
   * <p>{@code conversationId} is URL-encoded for you.
   */
  public JsonNode reply(String conversationId, Map<String, Object> params) {
    return client.post("/inbox/conversations/" + seg(conversationId) + "/reply", params);
  }

  /**
   * {@code POST /inbox/messages/:messageId/hide} - hide a comment someone
   * left on one of the user's posts, on the platform, as the post owner.
   * Equivalent to {@link #hide(String, boolean)} with {@code hide} =
   * {@code true}. Returns the updated message as {@code { data: InboxMessage
   * }} with its {@code hidden} flag flipped.
   *
   * <p>Facebook, Instagram, TikTok, YouTube and Threads comments (Threads:
   * incoming top-level replies only; Threads does not allow hiding nested
   * replies). On YouTube, hide sets the comment's moderation status to
   * rejected, which removes it and its replies from public view; unhide
   * publishes it again. The message keeps its place in the conversation, and
   * a hidden comment no longer counts as unanswered. The account must have
   * been connected with the moderation permission (Facebook
   * {@code pages_manage_engagement}, Instagram
   * {@code instagram_business_manage_comments}).
   *
   * <p>Errors: 400 {@code unsupported_platform} (not an incoming comment on
   * a supported platform), 400 {@code not_hideable} (Threads nested reply, or
   * Threads refused), 401 {@code reauth_required} (the Threads reply
   * permission or the TikTok comments authorization is missing or expired),
   * 403 {@code reconnect_required} (the account was connected without the
   * comment-moderation permission; reconnect it in the dashboard), 404
   * {@code not_found} (message not in this workspace) or
   * {@code account_not_connected}, 429 {@code quota_exceeded} (YouTube's
   * daily API quota is used up; retry after midnight Pacific), 502
   * {@code platform_error} (the platform rejected the call). Threads inbox
   * is currently rolling out; until Meta approves the permissions it is
   * disabled on production and Threads calls return a clear error.
   *
   * <p>{@code messageId} is URL-encoded for you.
   */
  public JsonNode hide(String messageId) {
    return client.post("/inbox/messages/" + seg(messageId) + "/hide");
  }

  /**
   * {@code POST /inbox/messages/:messageId/hide} - hide ({@code hide} =
   * {@code true}) or unhide ({@code hide} = {@code false}) a comment someone
   * left on one of the user's posts. See {@link #hide(String)} for the
   * platforms, rules and error codes.
   */
  public JsonNode hide(String messageId, boolean hide) {
    return client.post("/inbox/messages/" + seg(messageId) + "/hide", Map.of("hide", hide));
  }

  /**
   * {@code DELETE /inbox/messages/:messageId} - delete a comment someone left
   * on one of the user's posts, on the platform and from the inbox. Facebook,
   * Instagram and TikTok comments only: YouTube's API does not let a channel
   * delete other people's comments, hide those instead
   * ({@link #hide(String)}). Replies under the deleted comment go with it
   * (the platforms cascade the delete and the inbox mirrors that); their
   * inbox ids come back as {@code removed_reply_ids}. A comment that is
   * already gone on the platform is still removed from the inbox. This
   * cannot be undone. Returns {@code { data: { id, conversation_id,
   * removed_reply_ids } }}.
   *
   * <p>Errors: 400 {@code unsupported_platform} (not an incoming Facebook,
   * Instagram or TikTok comment), 401 {@code reauth_required} (the TikTok
   * comments authorization expired), 403 {@code reconnect_required} (the
   * account was connected without the comment-moderation permission;
   * reconnect it in the dashboard), 404 {@code not_found} (message not in
   * this workspace) or {@code account_not_connected}, 502
   * {@code platform_error} (the platform rejected the call).
   *
   * <p>{@code messageId} is URL-encoded for you.
   */
  public JsonNode deleteMessage(String messageId) {
    return client.delete("/inbox/messages/" + seg(messageId));
  }

  /**
   * {@code GET /inbox/next} - the next conversation that needs an answer,
   * using the default queue: the oldest unread item across all platforms and
   * types. See {@link #next(Map)}.
   */
  public JsonNode next() {
    return client.get("/inbox/next");
  }

  /**
   * {@code GET /inbox/next?platform=&type=&order=&include_read=&exclude=} -
   * the next conversation that needs an answer: a work queue for answering
   * the inbox. Returns the oldest (by default) item that still needs a reply,
   * together with its conversation so far and the post it belongs to, so a
   * reply can be drafted from one call. An item needs an answer when it is
   * the customer's latest DM with no reply after it (Instagram/Facebook DMs
   * within the 24-hour messaging window only, since Meta refuses replies
   * outside it), or a comment/mention that has not been replied to and is
   * not hidden. Replies typed in the native apps count as answers (they are
   * mirrored into the inbox), so a thread a colleague answered on their
   * phone is not served again. Instagram mentions are skipped (no reply
   * path). Looks at the last 30 days of activity. Requires the
   * {@code inbox:read} scope.
   *
   * <p>Query params (all optional): {@code platform} ({@code instagram} |
   * {@code facebook} | {@code linkedin} | {@code tiktok} | {@code youtube} |
   * {@code x} | {@code threads}), {@code type} ({@code dm} | {@code comment}
   * | {@code mention}), {@code order} ({@code oldest}, the default: the item
   * that has waited longest first; or {@code newest}), {@code include_read}
   * (boolean; by default only unread items are served, so marking a
   * conversation read with {@link #markRead(String)} is the durable way to
   * skip it), {@code exclude} (a session-local skip: conversation ids to
   * leave out of this call, up to 100, as a comma-separated string or any
   * {@link Iterable} of ids).
   *
   * <p>Returns {@code { data: InboxNextUnanswered | null, remaining: int }}.
   * {@code data} is {@code { conversation, message, messages }}, or JSON
   * {@code null} when nothing is waiting. {@code message} is the unanswered
   * incoming item itself (the customer's latest DM, or the specific comment):
   * its {@code id} is what {@link #hide(String)} and
   * {@link #deleteMessage(String)} take, its {@code conversation_id} is what
   * {@link #reply(String, Map)} takes. {@code messages} is the conversation
   * so far, oldest first (the most recent 50 messages for long DM threads).
   * {@code remaining} is the number of unanswered items still waiting after
   * this one (capped at 500), {@code 0} when {@code data} is null. To chain
   * the queue, pass {@code include_next} = {@code true} to
   * {@link #reply(String, Map)} and it returns the next item in the same
   * response. Errors: 400 {@code validation_error} (unknown platform, type or
   * order).
   */
  public JsonNode next(Map<String, Object> query) {
    Object exclude = query.get("exclude");
    if (exclude instanceof Iterable) {
      StringBuilder joined = new StringBuilder();
      for (Object id : (Iterable<?>) exclude) {
        if (joined.length() > 0) {
          joined.append(',');
        }
        joined.append(id);
      }
      Map<String, Object> copy = new LinkedHashMap<>(query);
      if (joined.length() == 0) {
        copy.remove("exclude");
      } else {
        copy.put("exclude", joined.toString());
      }
      query = copy;
    }
    return client.get("/inbox/next", query);
  }
}
