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
import java.util.*;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatWindowController implements Initializable {

    @FXML private ListView<User> chatListView;
    @FXML private VBox chatArea;
    @FXML private TextField messageInput;
    @FXML private Button sendButton;
    @FXML private Button addChatButton;

    private User loggedInUser;
    private UserApi userApi;
    private ChatApi chatApi;
    private ChatInfoApi chatInfoApi;

    // The currently open chat conversation
    private Chat currentChat;

    // Sidebar list of chat partners
    private final ObservableList<User> chatPartners = FXCollections.observableArrayList();

    // Keep track of all loaded chats so we can check duplicates
    private List<Chat> loadedChats = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sidebar shows user names
        chatListView.setItems(chatPartners);
        chatListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u==null ? null : u.getName());
            }
        });
        chatListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldU, newU) -> { if(newU!=null) openChatWith(newU); }
        );

        // Button handlers
        sendButton.setOnAction(e -> sendMessage());
        addChatButton.setOnAction(e -> openCreateChatDialog());
    }

    /** Must be called by launcher to wire APIs and then seed the sidebar */
    public void init(User loggedInUser,
                     UserApi userApi,
                     ChatApi chatApi,
                     ChatInfoApi chatInfoApi) {
        this.loggedInUser = loggedInUser;
        this.userApi      = userApi;
        this.chatApi      = chatApi;
        this.chatInfoApi  = chatInfoApi;

        loadUserChats();
    }

    /** Loads all existing chats for this user into sidebar and cache */
    private void loadUserChats() {
    chatApi.getChatsForUser(loggedInUser.getId())
        .thenAccept(response -> {
            if (response.getStatus() != Protocol.Status.CONN_OK) {
                Platform.runLater(() ->
                    showAlert("Error", "Server status: " + response.getStatus(), Alert.AlertType.ERROR)
                );
                return;
            }

            // Check that the payload was marked OK
            if (response.getPacket().getMetaData().getCommProtocol()
                    .filter(cp -> cp == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)
                    .isEmpty()) {
                Platform.runLater(() ->
                    showAlert("Error", "Unexpected protocol: " +
                        response.getPacket().getMetaData().getCommProtocol(), Alert.AlertType.ERROR)
                );
                return;
            }

            // Now unwrap – it’ll be a PersistentSet, so cast to Collection<Chat> first
            var payload = response.getPacket().getMetaData().getPayload();
            if (payload.isEmpty()) {
                System.err.println("loadUserChats: no payload");
                return;
            }

            @SuppressWarnings("unchecked")
            Collection<Chat> chatCollection = (Collection<Chat>) payload.get();

            // Copy into a List for your own use
            List<Chat> chats = new ArrayList<>(chatCollection);

            // Cache for duplicate‑chat checks
            this.loadedChats = chats;

            // Build sidebar partners
            List<User> partners = chats.stream()
                .flatMap(c -> c.getUsers().stream())
                .filter(u -> !u.getId().equals(loggedInUser.getId()))
                .distinct()
                .toList();

            Platform.runLater(() -> chatPartners.setAll(partners));
        })
        .exceptionally(ex -> {
            Platform.runLater(() ->
                showAlert("Error", "Failed to load chats: " + ex.getMessage(), Alert.AlertType.ERROR)
            );
            return null;
        });
}


    /**
     * If a conversation with that partner is already loaded, use it directly;
     * otherwise request or create it on the backend.
     */
    private void openChatWith(User partner) {
        // see if we already have a chat containing exactly {me, partner}
        Optional<Chat> existing = loadedChats.stream()
            .filter(c -> {
                Set<Long> ids = c.getUsers().stream()
                                  .map(User::getId)
                                  .collect(Collectors.toSet());
                return ids.equals(Set.of(loggedInUser.getId(), partner.getId()));
            })
            .findFirst();

        CompletableFuture<Protocol> future;
        if (existing.isPresent()) {
            // fetch full chat to get latest messages
            currentChat = existing.get();
            future = chatApi.getChat(currentChat.getId());
        } else {
            // ask server to create new chat
            future = chatApi.createChat(List.of(loggedInUser.getId(), partner.getId()));
        }

        future.thenAccept(proto -> {
            if(proto.getPacket().getMetaData().getCommProtocol().orElse(null)
                    == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                Chat c = (Chat)proto.getPacket().getMetaData()
                                    .getPayload().orElse(null);
                if(c!=null) {
                    currentChat = c;
                    if (!loadedChats.contains(c)) {
                        loadedChats.add(c);
                        // update sidebar if truly new
                        Platform.runLater(() ->
                            chatPartners.add(partner)
                        );
                    }
                    renderChat(c);
                }
            } else {
                showAlert("Error", "Could not open chat", Alert.AlertType.ERROR);
            }
        }).exceptionally(ex -> {
            showAlert("Error", "Chat error: " + ex.getMessage(), Alert.AlertType.ERROR);
            return null;
        });
    }

    /** Renders messages of a chat and scrolls to bottom */
    private void renderChat(Chat chat) {
        chatInfoApi.getAllMessages(chat.getId())
            .thenAccept(proto -> {
                if(proto.getPacket().getMetaData().getCommProtocol().orElse(null)
                        == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                    @SuppressWarnings("unchecked")
                    List<ChatInfo> msgs = (List<ChatInfo>)
                        proto.getPacket().getMetaData().getPayload().orElse(List.of());

                    Platform.runLater(() -> {
                        chatArea.getChildren().clear();
                        msgs.forEach(m -> {
                            Label lbl = new Label(m.getText());
                            lbl.getStyleClass().add(
                                m.getSentBy().getId().equals(loggedInUser.getId())
                                    ? "msg-sent" : "msg-received"
                            );
                            lbl.setOnMouseClicked(e -> openEditMessageDialog(m));
                            chatArea.getChildren().add(lbl);
                        });
                    });
                }
            });
    }

    /** Sends a new message, then reloads current chat */
    @FXML private void sendMessage() {
        if (currentChat==null) return;
        String text = messageInput.getText().trim();
        if(text.isEmpty()) return;

        chatInfoApi.createChatInfo(
            currentChat.getId(),
            loggedInUser.getId(),
            text
        ).thenAccept(proto -> {
            if(proto.getPacket().getMetaData().getCommProtocol().orElse(null)
                    == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                Platform.runLater(() -> {
                    messageInput.clear();
                    renderChat(currentChat);
                });
            } else {
                showAlert("Error","Send failed",Alert.AlertType.ERROR);
            }
        });
    }

    /** Opens the “Create Chat” dialog */
    @FXML private void openCreateChatDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/CreateChatDialog.fxml")
            );
            Parent root = loader.load();
            CreateChatDialogController dc = loader.getController();
            dc.init(userApi,
                    chatApi,
                    loggedInUser,
                    loadedChats,
                    newChat -> {
                        // once created, add to cache, sidebar and open it
                        loadedChats.add(newChat);
                        Platform.runLater(() -> {
                            chatPartners.addAll(
                                newChat.getUsers().stream()
                                       .filter(u -> !u.getId().equals(loggedInUser.getId()))
                                       .toList()
                            );
                            openChatWith(
                                newChat.getUsers().stream()
                                       .filter(u -> !u.getId().equals(loggedInUser.getId()))
                                       .findFirst().orElse(null)
                            );
                        });
                    });

            Stage dlg = new Stage();
            dlg.initOwner(chatListView.getScene().getWindow());
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("New Chat");
            dlg.setScene(new Scene(root));
            dlg.showAndWait();
        } catch(IOException ex) {
            showAlert("Error","Cannot open dialog: "+ex.getMessage(),Alert.AlertType.ERROR);
        }
    }

    /** Opens an edit‑message dialog; on save we reload the chat */
    private void openEditMessageDialog(ChatInfo info) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/EditMessage.fxml")
            );
            Parent root = loader.load();
            EditMessageController ctrl = loader.getController();
            ctrl.init(info, chatInfoApi, updated -> Platform.runLater(() -> renderChat(currentChat)));

            Stage dlg = new Stage();
            dlg.initOwner(chatArea.getScene().getWindow());
            dlg.initModality(Modality.APPLICATION_MODAL);
            dlg.setTitle("Edit Message");
            dlg.setScene(new Scene(root));
            dlg.showAndWait();
        } catch(IOException ex) {
            showAlert("Error","Cannot open edit dialog: "+ex.getMessage(),Alert.AlertType.ERROR);
        }
    }

    /** Generic FX alert helper */
    private void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.setTitle(title);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }
}
