package com.example;

import com.example.socket.server.ProtoServer;
import com.example.model.User;
import com.example.model.Chat;
import com.example.model.ChatInfo;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.controllers.AdminDashBoardController;
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
        // Start server in a daemon thread
        Thread serverThread = new Thread(this::initServer, "Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();

        

        // Set up API client and inject into controller
        FxProtoClient fxClient = new FxProtoClient(Config.SERVER_NAME, Config.PORT_NUMBER, "admin");
        ApiClient apiClient = new ApiClient(fxClient);

        // Load Admin Dashboard FXML
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/AdminDashBoard.fxml")
        );
        Parent root = loader.load();
        AdminDashBoardController controller = loader.getController();
        controller.init(apiClient);

        // Add stop button to bottom of dashboard
        Button stopBtn = new Button("Stop Server & Exit");
        stopBtn.setOnAction(evt -> {
            shutdownEverything();
            Platform.exit();
        });

        // Attach the stop button to the bottom if layout supports it
        if (root instanceof BorderPane) {
            ((BorderPane) root).setBottom(stopBtn);
        }

        // Show the window
        primaryStage.setTitle("Haze Chat — Admin Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    private void initServer() {
        haze = new ProtoServer(Config.PORT_NUMBER);
        userProvider = new ORMProvider<>(User.class);
        chatProvider = new ORMProvider<>(Chat.class);
        chatInfoProvider = new ORMProvider<>(ChatInfo.class);

        haze.spoolQuery(User.class, userProvider);
        haze.spoolQuery(Chat.class, chatProvider);
        haze.spoolQuery(ChatInfo.class, chatInfoProvider);
    }

    private void shutdownEverything() {
        if (haze != null) haze.stop();
        if (userProvider != null) userProvider.close();
        if (chatProvider != null) chatProvider.close();
        if (chatInfoProvider != null) chatInfoProvider.close();
    }

    @Override
    public void stop() throws Exception {
        shutdownEverything();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
