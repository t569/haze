package com.example.clientfx.api;

import com.example.model.Chat;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatApi {
    private final ApiClient api;

    public ChatApi(ApiClient api) {
        this.api = api;
    }

    public ApiClient getApi() {
        return this.api;
    }

    /** Retrieve a Chat between two users by their IDs */
    // TODO: rewrite this to retrieve chats between two users
    // public CompletableFuture<Protocol> getChatBetween(long first_id, long second_id) {
    //     Chat stub = new Chat();
    //     stub.addUser(new User() {{ setId(first_id); }});
    //     stub.addUser(new User() {{ setId(second_id); }});
    //     Protocol req = new Protocol(
    //         Protocol.Status.CONN_CONF,
    //         new Protocol.Packet(
    //             api.getFxProtoClient().getClient().getClientId(),
    //             api.getFxProtoClient().getClient().getHostName(),
    //             "GET_CHAT_BETWEEN",
    //             new Protocol.Packet.MetaData(
    //                 Protocol.Packet.MetaData.CommProtocol.GET_ALL_BY_OBJ,
    //                 stub,
    //                 Chat.class.getSimpleName()
    //             )
    //         )
    //     );
    //     return api.sendRecieve(req);
    // }

    /** Create a new Chat with the given list of user IDs */
   /** Create a new Chat with the given list of user IDs, with duplicate prevention */
public CompletableFuture<Protocol> createChat(List<Long> userIds, String name) {
        if (userIds == null || userIds.isEmpty()) {
            CompletableFuture<Protocol> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Must supply at least one user ID"));
            return failed;
        }

        // Step 1: Fetch existing chats for a representative user (e.g. the first in the list)
        Long representativeId = userIds.get(0);
        return getChatsForUser(representativeId).thenCompose(proto -> {
            // Extract the List<Chat> payload
            @SuppressWarnings("unchecked")
            Collection<Chat> existingChats = (Collection<Chat>) proto.getPacket()
                .getMetaData()
                .getPayload()
                .orElse(Collections.emptySet());

            // Build the new participant ID set for comparison
            Set<Long> newSet = new HashSet<>(userIds);

            // Check duplicates against each existing chat
            for (Chat c : existingChats) {
                Set<Long> ids = c.getUsers().stream()
                                .map(User::getId)
                                .collect(Collectors.toSet());
                if (ids.equals(newSet) || c.getName().equalsIgnoreCase(name)) {
                    CompletableFuture<Protocol> dup = new CompletableFuture<>();
                    dup.completeExceptionally(
                        new RuntimeException("Duplicate chat exists with same users or name."));
                    return dup;
                }
            }
            // Step 3: No duplicate found — build and send the CREATE request
            Chat chat = new Chat();
            chat.setName(name);
            chat.setCreatedAt(LocalDateTime.now());
            for (Long id : userIds) {
                User u = new User();
                u.setId(id);
                chat.addUser(u);
            }

            Protocol req = new Protocol(
                Protocol.Status.CONN_CONF,
                new Protocol.Packet(
                    api.getFxProtoClient().getClient().getClientId(),
                    api.getFxProtoClient().getClient().getHostName(),
                    "CREATE CHAT",
                    new Protocol.Packet.MetaData(
                        Protocol.Packet.MetaData.CommProtocol.POST,
                        chat,
                        Chat.class.getSimpleName()
                    )
                )
            );
            return api.sendRecieve(req);
        });
    }


    /** Retrieve all Chats for a given user ID */
    public CompletableFuture<Protocol> getChatsForUser(long userid)
    {
        User stub = new User();
        stub.setId(userid);

        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "GET ALL CHATS FOR USER",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.GET_ALL_BY_OBJ,
                    stub,
                    Chat.class.getSimpleName()
                )
            )
        );
        return api.sendRecieve(req);
    }


    public CompletableFuture<Protocol> getChatPartners(long chatId)
    {
        Chat chatStub = new Chat();
        chatStub.setId(chatId);

        Protocol req = new Protocol(
            Protocol.Status.CONN_CONF,
            new Protocol.Packet(
                api.getFxProtoClient().getClient().getClientId(),
                api.getFxProtoClient().getClient().getHostName(),
                "GET CHAT PARTNERS",
                new Protocol.Packet.MetaData(
                    Protocol.Packet.MetaData.CommProtocol.GET_ALL_BY_OBJ,
                    chatStub,
                    Chat.class.getSimpleName()
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

    // public CompletableFuture<List<User>> getChatPartners(long userId) {
    //     // Build a stub User with only id set
    //     User stub = new User();
    //     stub.setId(userId);

    //     // Build a GET_ALL_BY_OBJ request: key = "Chat", payload = stub User
    //     Protocol req = new Protocol(
    //         Protocol.Status.CONN_CONF,
    //         new Protocol.Packet(
    //             api.getFxProtoClient().getClient().getClientId(),
    //             api.getFxProtoClient().getClient().getHostName(),
    //             "GET_ALL_BY_OBJ Chats for User",
    //             new Protocol.Packet.MetaData(
    //                 Protocol.Packet.MetaData.CommProtocol.GET_ALL_BY_OBJ,
    //                 stub,
    //                 "Chat"  // since we're fetching Chat children of the User
    //             )
    //         )
    //     );

    //     // Send and parse the response
    //     return api.sendRecieve(req)
    //         .thenApply(response -> {
    //             // Only proceed if server indicates success
    //             if (response.getPacket().getMetaData().getCommProtocol().get()
    //                     != Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
    //                 throw new RuntimeException("Failed to fetch chats for user " + userId);
    //             }
    //             @SuppressWarnings("unchecked")
    //             List<Chat> chats = (List<Chat>) response.getPacket()
    //                                                    .getMetaData()
    //                                                    .getPayload()
    //                                                    .orElse(List.of());
    //             // From each Chat, collect all participants except the requesting user
    //             return chats.stream()
    //                 .flatMap(chat -> chat.getUsers().stream())
    //                 .filter(u -> u.getId() != userId)
    //                 .distinct()
    //                 .toList();
    //         });
    // }
}
