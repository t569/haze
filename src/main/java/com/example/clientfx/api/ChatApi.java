package com.example.clientfx.api;

import com.example.model.Chat;
import com.example.socket.server.Protocol;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChatApi {
    private final ApiClient api;

    public ChatApi(ApiClient api) {
        this.api = api;
    }

    public ApiClient getApi() {
        return this.api;
    }

    /** Create a new Chat with the given list of user IDs */
    public CompletableFuture<Protocol> createChat(List<Long> userIds) {
        // Build the Chat object with users referenced by ID
        Chat chat = new Chat();
        userIds.forEach(id -> {
            // For each user ID, add a placeholder User reference
            // (server will resolve to actual User entities)
            chat.addUser(new com.example.model.User() {{
                setId(id);
            }});
        });

        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "CREATE CHAT",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.POST,
                    chat,
                    chat.getClass().getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Retrieve a Chat by its ID */
    public CompletableFuture<Protocol> getChat(long id) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "GET CHAT",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.GET,
                    id,
                    Chat.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Update the participants of an existing Chat */
    public CompletableFuture<Protocol> updateChat(Chat chat) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "UPDATE CHAT",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.UPDATE,
                    chat,
                    chat.getClass().getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }

    /** Delete a Chat by its ID */
    public CompletableFuture<Protocol> deleteChat(long id) {
        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "DELETE CHAT",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.DELETE,
                    id,
                    Chat.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }
}
