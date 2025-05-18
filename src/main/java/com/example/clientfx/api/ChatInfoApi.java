package com.example.clientfx.api;

import com.example.model.Chat;
import com.example.model.ChatInfo;
import com.example.socket.server.Protocol;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class ChatInfoApi {
    private final ApiClient api;

    public ChatInfoApi(ApiClient api) {
        this.api = api;
    }

    public ApiClient getApi() {
        return this.api;
    }

    /**
     * Create a new ChatInfo (message) in a given chat.
     * @param chatId       ID of the chat to post into
     * @param sentByUserId ID of the user sending the message
     * @param text         Message text
     */
    public CompletableFuture<Protocol> createChatInfo(long chatId, long sentByUserId, String text) {
        // Build the ChatInfo with timestamp, sender, chat reference, and text
        ChatInfo info = new ChatInfo(Instant.now(), null, text);
        info.setSentBy(new com.example.model.User() {{ setId(sentByUserId); }});
        info.setChat(new com.example.model.Chat() {{ setId(chatId); }});

        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "CREATE CHATINFO",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.POST,
                    info,
                    ChatInfo.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Retrieve a ChatInfo (message) by its ID */
    public CompletableFuture<Protocol> getChatInfo(long id) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "GET CHATINFO",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.GET,
                    id,
                    ChatInfo.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Update an existing ChatInfo (e.g. editing message text) */
    public CompletableFuture<Protocol> updateChatInfo(ChatInfo info) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "UPDATE CHATINFO",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.UPDATE,
                    info,
                    ChatInfo.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Delete a ChatInfo (message) by its ID */
    public CompletableFuture<Protocol> deleteChatInfo(long id) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "DELETE CHATINFO",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.DELETE,
                    id,
                    ChatInfo.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    public CompletableFuture<Protocol> getMessage(long messageid)
    {
        return getChatInfo(messageid);
    }

    public CompletableFuture<Protocol> createMessage(long chatId, long sentByUserId, String text)
    {
        return createChatInfo(chatId, sentByUserId, text);
    }
    
    public CompletableFuture<Protocol> editMessage(ChatInfo chatinfo)
    {
        return updateChatInfo(chatinfo);
    }

    public CompletableFuture<Protocol> deleteMessage(long messageid)
    {
        return deleteChatInfo(messageid);
    }

    public CompletableFuture<Protocol> getAllMessages(long chatId) {
        Chat stub = new Chat(); stub.setId(chatId);
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "GET_ALL_MESSAGES",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.GET_ALL_BY_OBJ,
                    stub,
                    ChatInfo.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }
}
