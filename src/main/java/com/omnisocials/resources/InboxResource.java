package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
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
 *       Threads replies only: {@code true} when the reply is hidden on
 *       Threads, JSON {@code null} for every other platform/message.
 *       {@code permalink} links to the reply or mentioning post on the
 *       platform, when known ({@code null} otherwise).
 *   <li><b>InboxParticipant</b> ({@code id}, {@code name}, {@code username},
 *       {@code profile_picture}) - the person on the other side of a
 *       conversation, or a message's {@code sender}.
 *   <li><b>InboxPostRef</b> ({@code id}, {@code caption}, {@code thumbnail}) -
 *       the post a comment/mention is attached to; {@code null} for DMs.
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
   * {@code unread} (boolean), {@code limit} (1-100), {@code cursor} (an opaque
   * cursor from a previous response's {@code pagination.next_cursor}).
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
   * <p>Params: {@code text} (string, required), {@code attachment_url}
   * (optional public URL of a single media asset to attach),
   * {@code attachment_type} (optional; {@code image} | {@code video} |
   * {@code audio} | {@code file}, pair with {@code attachment_url}).
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
   * <p>{@code conversationId} is URL-encoded for you.
   */
  public JsonNode reply(String conversationId, Map<String, Object> params) {
    return client.post("/inbox/conversations/" + seg(conversationId) + "/reply", params);
  }

  /**
   * {@code POST /inbox/messages/:messageId/hide} - hide a reply someone left
   * on one of the user's Threads posts, as the post owner (Threads only for
   * now). Equivalent to {@link #hide(String, boolean)} with {@code hide} =
   * {@code true}. Returns the updated message as {@code { data: InboxMessage
   * }} with its {@code hidden} flag flipped.
   *
   * <p>Only incoming top-level replies can be hidden (Threads does not allow
   * hiding nested replies); the message keeps its place in the conversation.
   *
   * <p>Errors: 400 {@code unsupported_platform} (not an incoming Threads
   * reply, or Threads inbox not available yet), 400 {@code not_hideable}
   * (nested reply or Threads refused), 401 {@code reauth_required} (the
   * connection lacks the reply permission; reconnect Threads), 404
   * {@code not_found} (message not in this workspace) or
   * {@code account_not_connected} (no Threads account).
   *
   * <p>{@code messageId} is URL-encoded for you.
   */
  public JsonNode hide(String messageId) {
    return client.post("/inbox/messages/" + seg(messageId) + "/hide");
  }

  /**
   * {@code POST /inbox/messages/:messageId/hide} - hide ({@code hide} =
   * {@code true}) or unhide ({@code hide} = {@code false}) a reply someone
   * left on one of the user's Threads posts. See {@link #hide(String)} for
   * the rules and error codes.
   */
  public JsonNode hide(String messageId, boolean hide) {
    return client.post("/inbox/messages/" + seg(messageId) + "/hide", Map.of("hide", hide));
  }
}
