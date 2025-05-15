package com.example.clientfx.controller;
import com.example.clientfx.api.ApiClient;
import com.example.clientfx.api.UserApi;
import com.example.clientfx.controller.UserFormController;
import com.example.clientfx.controller.UserListController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import java.io.IOException;

// TODO: work on this class for the admin dashboard
public class AdminController {
    @FXML
    private StackPane contentPane;

    private UserApi userApi;

    /** Called once, right after FXML is loaded. */
    public void init(ApiClient apiClient) {
        // Wrap ApiClient in UserApi
        this.userApi = new UserApi(apiClient);
        // Show the user list by default
        showUserList();
    }

    /** Handler for the “New User” button. */
    @FXML
    private void onNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/user_form.fxml")
            );
            Parent formRoot = loader.load();

            // Initialize the form controller: no existing user, and onSaved reloads list
            UserFormController formCtrl = loader.getController();
            formCtrl.init(userApi, null, savedUser -> {
                // After creating, go back to list
                showUserList();
            });

            contentPane.getChildren().setAll(formRoot);
        } catch (IOException e) {
            showError("Unable to load user form:\n" + e.getMessage());
        }
    }

    /** Handler for the “List Users” button or after save/delete. */
    @FXML
    private void onListUsers() {
        showUserList();
    }

    private void showUserList() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/user_list.fxml")
            );
            Parent listRoot = loader.load();

            UserListController listCtrl = loader.getController();
            listCtrl.init(userApi, this::showUserList);

            contentPane.getChildren().setAll(listRoot);
        } catch (IOException e) {
            showError("Unable to load user list:\n" + e.getMessage());
        }
    }

    /** Pops up an error alert. */
    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(AlertType.ERROR, msg);
            a.showAndWait();
        });
    }
}
