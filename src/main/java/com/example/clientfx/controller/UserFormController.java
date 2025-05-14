package com.example.clientfx.controller;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.api.UserApi;
import com.example.clientfx.network.FxProtoClient;
import com.example.socket.server.Protocol;
import com.example.model.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UserFormController {
    @FXML private TextField nameField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private UserApi userApi;
    private User user;             // null = new user; non-null = editing
    private Consumer<User> onSaved; // callback to notify parent of saved user


    /** Call this after loading the FXML to set up dependencies. */
    public void init(ApiClient apiClient, User existing, Consumer<User> onSaved) {
        this.userApi = new UserApi(apiClient);
        this.user     = existing;
        this.onSaved  = onSaved;

        if (user != null) {
            // Edit mode
            nameField.setText(user.getName());
        }

        saveButton.setText(user == null ? "Create" : "Update");
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(evt -> onSave());
        cancelButton.setOnAction(evt -> onCancel());
    }

     private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Validation Error", "Name cannot be empty.", Alert.AlertType.WARNING);
            return;
        }

        CompletableFuture<Protocol> future;
        if (user == null) {
            // Create new
            future = userApi.createUser(name);
        } else {
            // Update existing
            user.setName(name);
            future = userApi.updateUser(user);
        }

        ApiClient.ui(future,
            resp -> {
                User saved = Protocol.unwrapEntityResponse(resp, User.class);
                Platform.runLater(() -> {
                    onSaved.accept(saved);
                    closeWindow();
                });
            },
            err -> Platform.runLater(() ->
                showAlert("Save Error", err.getMessage(), Alert.AlertType.ERROR)
            )
        );
    }

    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
