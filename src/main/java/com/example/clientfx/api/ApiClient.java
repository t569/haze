package com.example.clientfx.api;

import com.example.clientfx.network.FxProtoClient;
import com.example.socket.server.Protocol;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiClient {
    private final FxProtoClient fxClient;

    public ApiClient(FxProtoClient fxClient)
    {
        this.fxClient = fxClient;
    }

    /* Core send; returns a future of the raw Protocol */
    public CompletableFuture<Protocol> sendRecieve(Protocol req) {
        return fxClient.sendRecieveAsyncCrud(req);
    }

    public FxProtoClient getFxProtoClient()
    {
        return this.fxClient;
    }

    /* Helper to run UI updates on the JavaFX thread */
    // TODO: ask about this
    public static <T> void ui(CompletableFuture <T> future,
                                Consumer <T> onSuccess,
                                Consumer <Throwable> onError) {

            future.thenAccept(result -> 
                Platform.runLater(() -> onSuccess.accept(result)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> onError.accept(ex));
                        return null;
                    });
    }
}
