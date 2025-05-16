package com.example;

import com.example.Config;
import com.example.socket.server.ProtoServer;
import com.example.model.User;
import com.example.model.Chat;
import com.example.model.ChatInfo;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.controllers.AdminController;
import com.example.clientfx.network.FxProtoClient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ServerClientApp extends Application {

    private ProtoServer haze;
    private ORMProvider<User, Long> userProvider;
    private ORMProvider<Chat, Long> chatProvider;
    private ORMProvider<ChatInfo, Long> chatInfoProvider;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Kick off the server on a daemon thread
        Thread serverThread = new Thread(this::initServer, "Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();

        // Load your existing admin dashboard FXML
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/main_admin.fxml")
        );
        Parent root = loader.load();

        // Set up the API client exactly as ClientApp did
        FxProtoClient fxClient =
            new FxProtoClient(Config.SERVER_NAME, Config.PORT_NUMBER, "admin");
        ApiClient apiClient = new ApiClient(fxClient);
        AdminController controller = loader.getController();
        controller.init(apiClient);

        // Add a “Stop Server & Exit” button at the bottom
        Button stopBtn = new Button("Stop Server & Exit");
        stopBtn.setOnAction(evt -> {
            shutdownEverything();
            Platform.exit();
        });

        // If your root layout is a BorderPane—swap in the correct pane type if not
        if (root instanceof BorderPane) {
            ((BorderPane) root).setBottom(stopBtn);
        }

        // Show the stage
        primaryStage.setTitle("Haze Chat — Admin Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    /** Initializes ProtoServer and all ORMProviders */
    private void initServer() {
        haze             = new ProtoServer(Config.PORT_NUMBER);
        userProvider     = new ORMProvider<>(User.class);
        chatProvider     = new ORMProvider<>(Chat.class);
        chatInfoProvider = new ORMProvider<>(ChatInfo.class);

        haze.spoolQuery(User.class, userProvider);
        haze.spoolQuery(Chat.class, chatProvider);
        haze.spoolQuery(ChatInfo.class, chatInfoProvider);
    }

    /** Stops the server thread and closes all ORMProviders */
    private void shutdownEverything() {
        if (haze != null)             haze.stop();
        if (userProvider != null)     userProvider.close();
        if (chatProvider != null)     chatProvider.close();
        if (chatInfoProvider != null) chatInfoProvider.close();
    }

    /** Ensure clean stop if user closes the window via the window-manager */
    @Override
    public void stop() throws Exception {
        shutdownEverything();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
