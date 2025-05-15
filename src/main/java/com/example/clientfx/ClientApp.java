package com.example.clientfx;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.api.ChatApi;
import com.example.clientfx.api.ChatInfoApi;
import com.example.clientfx.api.UserApi;
import com.example.clientfx.network.FxProtoClient;
import com.example.socket.server.ProtoClient;
import com.example.Config;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ClientApp extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception{

        // Connect to the backend

        // FxProtoClient fxProtoClient = new FxProtoClient(Config.SERVER_NAME, Config.PORT_NUMBER, "ClientApp");
        // ApiClient appClient = new ApiClient(fxProtoClient);

        // UserApi userApi = new UserApi(appClient);
        // ChatApi chatApi = new ChatApi(appClient);
        // ChatInfoApi chatInfoApi = new ChatInfoApi(appClient);

        // FXMLLoader loader = new FXMLLoader(getClass().getResource("views/main.fxml"));
        // Scene scene = new Scene(loader.load());


        // Inject API into controller
        // TODO: make this work
        // com.example.clientfx.controller.UserListController controller = loader.getController();
        // controller.setUserApi(userApi);

        // primaryStage.setTitle("Haze Chat - User Dashboard");
        // primaryStage.setScene(scene);
        // primaryStage.show();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Haze Chat - Main");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
