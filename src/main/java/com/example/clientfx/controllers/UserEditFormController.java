package com.example.clientfx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import com.example.clientfx.api.UserApi;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.util.function.Consumer;

public class UserEditFormController {
    @FXML private TextField nameField;
    @FXML private Button saveButton;

    private User user;
    private UserApi userApi;
    private Consumer<User> onSaved;

    /** Initialize with the user to edit, the API, and a callback */
    public void init(User user, UserApi api, Consumer<User> onSaved) {
        this.user = user;
        this.userApi = api;
        this.onSaved = onSaved;

        nameField.setText(user.getName());
        saveButton.setOnAction(evt -> saveChanges());
    }

    private void saveChanges() {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Name cannot be blank", ButtonType.OK)
                .showAndWait();
            return;
        }
        user.setName(newName);  // apply change to detached entity
        userApi.updateUser(user).thenAccept(response -> {
            if (response.getStatus() == Protocol.Status.CONN_OK) {
                onSaved.accept(user);
            } else {
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Update failed", ButtonType.OK)
                        .showAndWait()
                );
            }
        }).exceptionally(ex -> {
            Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage(), ButtonType.OK)
                    .showAndWait()
            );
            return null;
        });
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
