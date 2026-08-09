package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;
import java.util.Map;

/**
 * Social inbox: DMs, comments, and mentions across connected platforms
 * (Instagram, Facebook, LinkedIn, and X DMs). Accessed via {@code client.inbox()}.
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
 *       {@code post}).
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
   * {@code x}),
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
   * <p>X DM replies cost 2 prepaid credits per send, debited from the company
   * balance before the message is sent and automatically refunded if the send
   * fails. This can throw an {@link com.omnisocials.errors.ApiException} with
   * status 402 and code {@code insufficient_credits} (the balance can't cover
   * the 2 credits) or {@code x_inbox_suspended} (the workspace's X inbox was
   * auto-suspended after hitting a zero balance; top up and re-enable it in
   * the dashboard to resume - DMs that arrived while suspended are not
   * recovered). Replies on other platforms are free.
   *
   * <p>{@code conversationId} is URL-encoded for you.
   */
  public JsonNode reply(String conversationId, Map<String, Object> params) {
    return client.post("/inbox/conversations/" + seg(conversationId) + "/reply", params);
  }
}
