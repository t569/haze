package com.example.clientfx.controllers;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.clientfx.api.ChatApi;
import com.example.clientfx.api.UserApi;
import com.example.model.Chat;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CreateChatDialogController {

    @FXML private ListView<User> availableUsersList;
    @FXML private TextField chatNameField;
    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private UserApi userApi;
    private ChatApi chatApi;
    private User self;

    // must be observablelist for auto update Ui
    private ObservableList<Chat> existingChats;
    private Consumer<Chat> onChatCreated;

    private final ObservableList<User> allUsers = FXCollections.observableArrayList();

    /**
     * Initialize the dialog.
     */
    public void init(UserApi userApi,
                     ChatApi chatApi,
                     User self,
                     ObservableList<Chat> existingChats,
                     Consumer<Chat> onChatCreated) {
        this.userApi       = userApi;
        this.chatApi       = chatApi;
        this.self          = self;
        this.existingChats = existingChats;
        this.onChatCreated = onChatCreated;

        // Enable multiple selection of users
        availableUsersList.setItems(allUsers);
        availableUsersList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Auto-fill and edit name field based on selection count
        availableUsersList.getSelectionModel().getSelectedItems().addListener((ListChangeListener<User>) change -> {
            List<User> sel = availableUsersList.getSelectionModel().getSelectedItems();
            if (sel.size() == 1) {
                // For one-on-one chat, auto name and make read-only
                chatNameField.setText(self.getName() + " & " + sel.get(0).getName());
                chatNameField.setEditable(false);
            } else {
                // For group chats, clear and allow editing
                chatNameField.clear();
                chatNameField.setEditable(true);
            }
        });

        // Display user names only
        availableUsersList.setCellFactory(avuserlist -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText((empty || user == null) ? null : user.getName());
            }
        });
        loadAllUsers();
    }

    private void loadAllUsers() {
        userApi.getAllUsers()
            .thenAccept(proto -> {
                if (proto.getPacket().getMetaData().getCommProtocol().orElse(null)
                        == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    @SuppressWarnings("unchecked")
                    List<User> users = (List<User>) proto.getPacket()
                        .getMetaData().getPayload().orElse(List.of());
                    // Exclude self
                    users.removeIf(u -> u.getId().equals(self.getId()));
                    Platform.runLater(() -> allUsers.setAll(users));
                }
            });
    }

    @FXML
    private void onCreateChat() {
        List<User> selected = availableUsersList.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("Validation", "Please select at least one other user.", Alert.AlertType.WARNING);
            return;
        }
        String name = chatNameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Validation", "Please enter a chat name.", Alert.AlertType.WARNING);
            return;
        }

        // Build participant ID set
        Set<Long> newSet = new HashSet<>();
        newSet.add(self.getId());
        selected.forEach(u -> newSet.add(u.getId()));

        // Prevent duplicate chat for exact participants
        boolean dup = existingChats.stream().anyMatch(chat -> {
            Set<Long> ids = chat.getUsers().stream()
                                 .map(User::getId)
                                 .collect(Collectors.toSet());
            return ids.equals(newSet);
        });
        if (dup) {
            showAlert("Duplicate", "A chat with exactly these users already exists.", Alert.AlertType.ERROR);
            return;
        }

        // Create chat via API
        chatApi.createChat(new ArrayList<>(newSet), name)
            .thenAccept(proto -> {
                if (proto.getPacket().getMetaData().getCommProtocol().orElse(null)
                        == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    Chat created = (Chat) proto.getPacket()
                                       .getMetaData()
                                       .getPayload()
                                       .orElse(null);
                    if (created != null) {
                        Platform.runLater(() -> {
                            onChatCreated.accept(created);
                            existingChats.add(created);
                            closeWindow();
                        });
                    }
                } else {
                    showAlert("Error", "Server refused to create chat.", Alert.AlertType.ERROR);
                }
            })
            .exceptionally(ex -> {
                showAlert("Error", "Chat creation failed: " + ex.getMessage(), Alert.AlertType.ERROR);
                return null;
            });
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage st = (Stage) availableUsersList.getScene().getWindow();
        st.close();
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }
}
