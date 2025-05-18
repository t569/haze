package com.example.clientfx.controllers;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.clientfx.api.UserApi;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.util.List;
import java.util.function.Consumer;

public class AddChatUserController {
    @FXML private ListView<User> userListView;
    @FXML private Button addButton;

    private UserApi userApi;
    private User self;
    private Consumer<User> onUserSelected;
    private final ObservableList<User> users = FXCollections.observableArrayList();

    /**
     * Initialize with current user and callback.
     */
    public void init(UserApi userApi, User self, Consumer<User> onUserSelected) {
        this.userApi = userApi;
        this.self = self;
        this.onUserSelected = onUserSelected;

        userListView.setItems(users);
        userListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 2) Custom cell factory so each cell shows user.getName()
        userListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getName());      // or getUsername()
                }
            }
        });

        // wire up the add button
        addButton.setOnAction(evt -> addSelectedUser());

        loadAvailableUsers();
    }

    private void loadAvailableUsers() {
        userApi.getAllUsers().thenAccept(response -> {
            if (response.getPacket().getMetaData().getCommProtocol().orElse(null)
                    == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                @SuppressWarnings("unchecked")
                List<User> list = (List<User>) response.getPacket()
                    .getMetaData().getPayload().orElse(List.of());
                Platform.runLater(() -> {
                    // Exclude self
                    list.removeIf(u -> u.getId().equals(self.getId()));
                    users.setAll(list);
                });
            }
        });
    }

    /**
     * Add selected user to chat.
     */
    @FXML
    private void addSelectedUser() {
        User selected = userListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            onUserSelected.accept(selected);
            close();
        } else {
            new Alert(Alert.AlertType.WARNING, "Please select a user.", ButtonType.OK)
                .showAndWait();
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) userListView.getScene().getWindow();
        stage.close();
    }
}
