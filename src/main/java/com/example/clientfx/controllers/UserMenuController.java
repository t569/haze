package com.example.clientfx.controllers;

import com.example.model.Chat;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.io.IOException;

import com.example.clientfx.api.ChatApi;
import com.example.clientfx.api.ChatInfoApi;
import com.example.clientfx.api.UserApi;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class UserMenuController {
    @FXML private Label userLabel;
    @FXML private Button deleteButton;
    @FXML private Button editButton;
    @FXML private Button loginButton;


    private User user;
    private UserApi userApi;
    private Runnable onDone;


    public void init(User user, UserApi api, Runnable onDone)
    {
        this.user = user;
        this.userApi = api;
        this.onDone = onDone;

        // error
        userLabel.setText(user.getName());

        deleteButton.setOnAction(evt -> deleteUser());
        editButton.setOnAction(evt -> editUser());
        loginButton.setOnAction(evt -> loginUser());
    }

    private void deleteUser() {
    userApi.deleteUser(user.getId()).thenAccept(response -> {
        if (response.getStatus() == Protocol.Status.CONN_OK) {
            // Run on the FX thread:
            Platform.runLater(() -> {
                closeWindow();   // first close the menu dialog…
                onDone.run();    // …then reload the dashboard’s user list
            });
        } else {
            Platform.runLater(() ->
                showAlert("Error", "Delete failed", Alert.AlertType.ERROR)
            );
        }
    }).exceptionally(ex -> {
        Platform.runLater(() ->
            showAlert("Error", ex.getMessage(), Alert.AlertType.ERROR)
        );
        return null;
    });
}


    // TODO: write this
    private void editUser()
    {
        // the general work flow is: get the user, make changes, call update on the user

        try 
        {
            // load the FXML for editing the user
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/UserEditForm.fxml")
            );
            Parent root = loader.load();
            
            // get its controller and initiliase the form
            UserEditFormController formCtrl = loader.getController();
            formCtrl.init(user, userApi, updatedUser -> {
                // On successful update: refresh dashboard and close
                Platform.runLater(() -> {
                    onDone.run();                   // reload the main table
                    showAlert("Success", "User updated successfully", Alert.AlertType.INFORMATION);
                    closeWindow();                  // close the edit window
                    onDone.run();
                });
            });

            // what is shown in the modal dialog
            Stage dialog = new Stage();
            dialog.initOwner(userLabel.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Edit User: " + user.getName());
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        }catch(Exception e)
        {
            showAlert("Error", "Unable to load edit user form: " + e.getMessage(), Alert.AlertType.ERROR);
        }
        

    }

    // TODO: URGENT, fix this, it doesnt work
    private void loginUser()
    {
        // Open ChatWindow.fxml with this users context
        try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/ChatWindow.fxml")
        );
        Parent root = loader.load();
        ChatWindowController ctrl = loader.getController();

        // construct ChatApi using the user api, chat api and chatinfo api
        ChatApi chatApi = new ChatApi(userApi.getApi());
        ChatInfoApi chatInfoApi = new ChatInfoApi(userApi.getApi());

        // initialize the controller with the user, userApi, chatInfoApi and chatApi
        ctrl.init(user, userApi,chatApi, chatInfoApi); 

        Stage stage = new Stage();
        stage.initOwner(userLabel.getScene().getWindow());
        stage.initModality(Modality.NONE);
        stage.setTitle("Chat — " + user.getName());
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot open chat window: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void closeWindow()
    {
        Stage stage = (Stage) userLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
