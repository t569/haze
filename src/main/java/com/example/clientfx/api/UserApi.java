package com.example.clientfx.api;

import com.example.model.User;
import com.example.socket.server.Protocol;
import java.util.concurrent.CompletableFuture;


public class UserApi {
    private final ApiClient api;

    public UserApi(ApiClient api) {
        this.api = api;
    }

    public ApiClient getApi()
    {
        return this.api;
    }

    public CompletableFuture<Protocol> createUser(String username)
    {
        // Initialize the user object

        // TODO: how do i add chats and other stuff?

        // build the request

        
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "CREATE USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.POST,
                                    new User(username),
                                    "User"
                                )
                            )
        );


        // send recieve the ting

        return api.sendRecieve(req);
    }


    // only field to update is the name
    public CompletableFuture<Protocol> updateUser(String username)
    {
        User user_updated = new User(username);
        // build the request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "UPDATE USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.UPDATE,
                                    user_updated,
                                    user_updated.getClass().getSimpleName()
                                    
                                )
                            )
        );

        // send recieve the ting
        return api.sendRecieve(req);
    }

    // overload update user function to work for user objects aswell
    public CompletableFuture<Protocol> updateUser(User user)
    {
        // build the request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "UPDATE USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.UPDATE,
                                    user,
                                    user.getClass().getSimpleName()
                                )
                            )
        );

        // send recieve the ting
        return api.sendRecieve(req);
    }

    
    public CompletableFuture<Protocol> deleteUser(long id)
    {
        // this is a dummy
        User user = new User();


        // build the request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "DELETE USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.UPDATE,
                                    id,
                                    user.getClass().getSimpleName()
                                )
                            )
        );

        // send recieve the ting
        return api.sendRecieve(req);
    }

    // get user by id
    public CompletableFuture<Protocol> getUser(long id)
    {
         // this is a dummy
        User user = new User();


        // build request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "GET USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.GET,
                                    id,
                                    user.getClass().getSimpleName()
                                )
                            )
        );  
        
        // send the request recieve the response
        return api.sendRecieve(req);
    } 

}

    