package com.example.clientfx.controllers;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.*;

import com.example.clientfx.api.ChatApi;
import com.example.clientfx.api.ChatInfoApi;
import com.example.clientfx.api.UserApi;
import com.example.model.Chat;
import com.example.model.ChatInfo;
import com.example.model.User;
import com.example.socket.server.Protocol;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatWindowController implements Initializable {

    @FXML private ListView<Chat> chatListView;
    @FXML private VBox       chatArea;
    @FXML private TextField  messageInput;
    @FXML private Button     sendButton;
    @FXML private Button     addChatButton;

    private User          loggedInUser;
    private UserApi       userApi;
    private ChatApi       chatApi;
    private ChatInfoApi   chatInfoApi;

    private Chat currentChat;
    private final ObservableList<Chat> loadedChats  = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1) UI wiring only—no access to loggedInUser yet:
        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Chat chat, boolean empty) {
                super.updateItem(chat, empty);
                setText(empty || chat == null ? null : chat.getName() != null && !chat.getName().isBlank()
                    ? chat.getName()
                    : chat.getUsers().stream()
                          .map(User::getName)
                          .collect(Collectors.joining(", "))
                );
            }
        });

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldC, newC) -> {
            if (newC != null) {
                openChat(newC);
            }
        });

        sendButton.setOnAction(e -> sendMessage());
        addChatButton.setOnAction(e -> openCreateChatDialog());
    }

    /**
     * MUST be called immediately after FXMLLoader.load() to set up APIs and
     * then drive initial data load.
     */
    public void init(User loggedInUser,
                     UserApi userApi,
                     ChatApi chatApi,
                     ChatInfoApi chatInfoApi) {
        this.loggedInUser = loggedInUser;
        this.userApi      = userApi;
        this.chatApi      = chatApi;
        this.chatInfoApi  = chatInfoApi;

        // Now load the chats into the sidebar
        loadUserChats();
    }

    private void loadUserChats() {
        chatApi.getChatsForUser(loggedInUser.getId())
            .thenAccept(response -> {
                if (response.getStatus() != Protocol.Status.CONN_OK) {
                    Platform.runLater(() ->
                        showAlert("Error", "Server status: " + response.getStatus(), Alert.AlertType.ERROR)
                    );
                    return;
                }

                var comm = response.getPacket().getMetaData().getCommProtocol();
                if (comm.isEmpty() || comm.get() != Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    Platform.runLater(() ->
                        showAlert("Error", "Unexpected protocol: " + comm, Alert.AlertType.ERROR)
                    );
                    return;
                }

                var payload = response.getPacket().getMetaData().getPayload();
                if (payload.isEmpty()) return;

                @SuppressWarnings("unchecked")
                Collection<Chat> set = (Collection<Chat>) payload.get();

                Platform.runLater(() -> {
                    loadedChats.setAll(set);
                    chatListView.setItems(loadedChats);
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() ->
                    showAlert("Error", "Failed to load chats: " + ex.getMessage(), Alert.AlertType.ERROR)
                );
                return null;
            });
    }

    /** Fetches/up‑dates the given chat then renders its messages. */
    private void openChat(Chat chat) {
        chatApi.getChat(chat.getId())
            .thenAccept(response -> {
                if (response.getStatus() != Protocol.Status.CONN_OK) {
                    Platform.runLater(() ->
                        showAlert("Error", "Server error: " + response.getStatus(), Alert.AlertType.ERROR)
                    );
                    return;
                }

                var comm = response.getPacket().getMetaData().getCommProtocol();
                if (comm.isEmpty() || comm.get() != Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    Platform.runLater(() ->
                        showAlert("Error", "Protocol error: " + comm, Alert.AlertType.ERROR)
                    );
                    return;
                }

                @SuppressWarnings("unchecked")
                Chat fresh = (Chat) response.getPacket().getMetaData().getPayload().orElse(null);
                if (fresh != null) {
                    currentChat = fresh;
                    renderChat(fresh);
                }
            })
            .exceptionally(ex -> {
                Platform.runLater(() ->
                    showAlert("Error", "Failed to open chat: " + ex.getMessage(), Alert.AlertType.ERROR)
                );
                return null;
            });
    }

    /** Renders all ChatInfo messages for the active `currentChat`. */
    private void renderChat(Chat chat) {
        chatInfoApi.getAllMessages(chat.getId())
            .thenAccept(response -> {
                var comm = response.getPacket().getMetaData().getCommProtocol().orElse(null);
                if (comm != Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    Platform.runLater(() ->
                        showAlert("Error", "Load messages failed: " + comm, Alert.AlertType.ERROR)
                    );
                    return;
                }

                @SuppressWarnings("unchecked")
                List<ChatInfo> msgs = (List<ChatInfo>)
                    response.getPacket().getMetaData().getPayload().orElse(List.of());

                Platform.runLater(() -> {
                    chatArea.getChildren().clear();
                    for (ChatInfo m : msgs) {
                        Label lbl = new Label(m.getText());
                        lbl.getStyleClass().add(
                            m.getSentBy().getId().equals(loggedInUser.getId())
                                ? "msg-sent" : "msg-received"
                        );
                        lbl.setOnMouseClicked(e -> openEditMessageDialog(m));
                        chatArea.getChildren().add(lbl);
                    }
                });
            });
    }

    /** Sends a new message, then re‑renders the current chat. */
    @FXML private void sendMessage() {
        if (currentChat == null) return;
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        chatInfoApi.createChatInfo(currentChat.getId(), loggedInUser.getId(), text)
            .thenAccept(response -> {
                var comm = response.getPacket().getMetaData().getCommProtocol().orElse(null);
                if (comm == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    Platform.runLater(() -> {
                        messageInput.clear();
                        renderChat(currentChat);
                    });
                } else {
                    Platform.runLater(() ->
                        showAlert("Error", "Send failed: " + comm, Alert.AlertType.ERROR)
                    );
                }
            });
    }

    /** Pops up the “Create Chat” dialog; on success, adds and opens it. */
    @FXML private void openCreateChatDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CreateChatDialog.fxml"));
            Parent root    = loader.load();
            CreateChatDialogController dc = loader.getController();
            dc.init(userApi, chatApi, loggedInUser, loadedChats, newChat -> {
                loadedChats.add(newChat);
                Platform.runLater(() -> {
                    chatListView.getSelectionModel().select(newChat);
                    openChat(newChat);
                });
            });

            Stage dlg = new Stage();
            dlg.initOwner(chatListView.getScene().getWindow());
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Chat");
            dlg.setScene(new Scene(root));
            dlg.showAndWait();

        } catch (IOException ex) {
            showAlert("Error", "Cannot open dialog: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Pop up edit‐message window; on save, re‑render current chat. */
    private void openEditMessageDialog(ChatInfo info) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EditMessage.fxml"));
            Parent root = loader.load();
            EditMessageController ctrl = loader.getController();
            ctrl.init(info, chatInfoApi, updated -> Platform.runLater(() -> renderChat(currentChat)));

            Stage dlg = new Stage();
            dlg.initOwner(chatArea.getScene().getWindow());
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Edit Message");
            dlg.setScene(new Scene(root));
            dlg.showAndWait();
        } catch (IOException ex) {
            showAlert("Error", "Cannot open edit dialog: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /** Thread‑safe FX alert helper. */
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }
}
