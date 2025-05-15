package com.example;

import com.example.model.*;
import com.example.socket.server.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ServerUI extends Application {

    private ProtoServer haze;
    private ORMProvider<User, Long> userProvider;
    private ORMProvider<Chat, Long> chatProvider;
    private ORMProvider<ChatInfo, Long> chatInfoProvider;

    @Override
    public void start(Stage primaryStage) {
        // Start server in background thread
        new Thread(this::startServer, "Server-Thread").start();

        // UI
        Button stopButton = new Button("Stop Server & Exit");
        stopButton.setOnAction(event -> {
            stopServer();
            Platform.exit();
        });

        StackPane root = new StackPane(stopButton);
        Scene scene = new Scene(root, 300, 200);
        primaryStage.setTitle("Server Control");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startServer() {
        haze = new ProtoServer(8080);
        userProvider = new ORMProvider<>(User.class);
        chatProvider = new ORMProvider<>(Chat.class);
        chatInfoProvider = new ORMProvider<>(ChatInfo.class);

        haze.spoolQuery(User.class, userProvider);
        haze.spoolQuery(Chat.class, chatProvider);
        haze.spoolQuery(ChatInfo.class, chatInfoProvider);
    }

    private void stopServer() {
        if (haze != null) haze.stop();
        if (userProvider != null) userProvider.close();
        if (chatProvider != null) chatProvider.close();
        if (chatInfoProvider != null) chatInfoProvider.close();
    }

    @Override
    public void stop() throws Exception {
        stopServer();
        super.stop();
    }
}
