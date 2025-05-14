package com.example.clientfx.network;

import com.example.socket.server.Protocol;
import com.example.socket.server.ProtoClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;


// wrap my protoclient in a CompletableFuture to expose an API
public class FxProtoClient {
    
    private final ProtoClient client;

    public FxProtoClient(String host, int port, String clientid) throws IOException{
        // note this handles the handshake and socket creation
        this.client = new ProtoClient(host, port, clientid);
    }

    public ProtoClient getClient()
    {
        return this.client;
    }
    
    /** Send a Protocol.Request asynchronously and receive the Protocol.Response. */
    public CompletableFuture<Protocol> sendRecieveAsyncCrud(Protocol req) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                client.sendRequest(req);
                Protocol response =  client.getResponse(client.getInStream());
                return response;
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // TODO: write a send async method for server disconnection

}
