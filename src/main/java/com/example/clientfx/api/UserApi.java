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
                                    User.class.getSimpleName()
                                )
                            )
        );


        // send recieve the ting

        return api.sendRecieve(req);
    }

    public CompletableFuture<Protocol> createUser(User user)
    {
        Protocol req = new Protocol(
                        Protocol.Status.CONN_CONF,
                        new Protocol.Packet(
                            api.getFxProtoClient().getClient().getClientId(),
                            api.getFxProtoClient().getClient().getHostName(),
                            "CREATE USER",
                            new Protocol.Packet.MetaData(
                                Protocol.Packet.MetaData.CommProtocol.POST,
                                user,
                                User.class.getSimpleName()
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
                                    User.class.getSimpleName()
                                    
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
                                    User.class.getSimpleName()
                                )
                            )
        );

        // send recieve the ting
        return api.sendRecieve(req);
    }

    // public CompletableFuture<Protocol> updateUser(long id)
    // {
    //     // fetch user with id
    //     // build the request
    //     Protocol req = new Protocol(
    //                         Protocol.Status.CONN_CONF,
    //                         new Protocol.Packet(
    //                             api.getFxProtoClient().getClient().getClientId(),
    //                             api.getFxProtoClient().getClient().getHostName(),
    //                             "UPDATE USER",
    //                             new Protocol.Packet.MetaData(
    //                                 Protocol.Packet.MetaData.CommProtocol.UPDATE,
    //                                 user,
    //                                 user.getClass().getSimpleName()
    //                             )
    //                         )
    //     );

    // }
    
    public CompletableFuture<Protocol> deleteUser(long id)
    {
        // build the request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "DELETE USER",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.DELETE,
                                    id,
                                    User.class.getSimpleName()
                                )
                            )
        );

        // send recieve the ting
        return api.sendRecieve(req);
    }

    // get user by id
    public CompletableFuture<Protocol> getUser(long id)
    {
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
                                    User.class.getSimpleName()
                                )
                            )
        );  
        
        // send the request recieve the response
        return api.sendRecieve(req);
    } 

    // get all users
    public CompletableFuture<Protocol> getAllUsers()
    {
        // this is a dummy
        User user = new User();

        // build request
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "GET ALL USERS",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.GET_ALL,
                                    null,
                                    user.getClass().getSimpleName()
                                )
                            )
        );  
        
        // send the request recieve the response
        return api.sendRecieve(req);
    }

    // delete all the users
    public CompletableFuture<Protocol> deleteAllUsers()
    {
        Protocol req = new Protocol(
                            Protocol.Status.CONN_CONF,
                            new Protocol.Packet(
                                api.getFxProtoClient().getClient().getClientId(),
                                api.getFxProtoClient().getClient().getHostName(),
                                "DELETE ALL USERS",
                                new Protocol.Packet.MetaData(
                                    Protocol.Packet.MetaData.CommProtocol.DELETE_ALL,
                                    null,
                                    User.class.getSimpleName()
                                )
                            )
        );

        // send recieve the request
        return api.sendRecieve(req);
    }

}
    