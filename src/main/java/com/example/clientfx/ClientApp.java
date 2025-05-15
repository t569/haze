package com.example.clientfx;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.api.ChatApi;
import com.example.clientfx.api.ChatInfoApi;
import com.example.clientfx.api.UserApi;
import com.example.clientfx.network.FxProtoClient;
import com.example.socket.server.ProtoClient;
import com.example.clientfx.controller.AdminController;
import com.example.Config;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;


public class ClientApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main_admin.fxml"));
        Parent root = loader.load();

        // Inject APIs
        FxProtoClient fx = new FxProtoClient(Config.SERVER_NAME, Config.PORT_NUMBER, "admin");
        ApiClient apiClient = new ApiClient(fx);
        AdminController ctrl = loader.getController();
        ctrl.init(apiClient);

        primaryStage.setTitle("Haze Chat — Admin Dashboard");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    public static void main(String[] args) { launch(args); }
}
