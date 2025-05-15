// package com.example.clientfx.controller;

// import java.util.stream.Collectors;
// import java.util.List;
// import javafx.scene.control.ListView;
// import com.example.clientfx.api.ChatApi;
// import com.example.socket.server.Protocol;
// import com.example.clientfx.api.ChatInfoApi;
// import com.example.model.Chat;
// import com.example.model.ChatInfo;
// import com.example.model.User;

// import javafx.application.Platform;
// import javafx.collections.FXCollections;
// import javafx.fxml.FXML;
// import javafx.scene.control.TextField;

// public class ChatWindowController {
//     @FXML private ListView<User> peerList;
//     @FXML private ListView<ChatInfo> messageList;
//     @FXML private TextField msgField;

//     private User me;
//     private ChatApi chatApi;
//     private ChatInfoApi infoApi;
//     private Chat currentChat;

//     public void init(User me, ChatApi chatApi, ChatInfoApi infoApi) {
//         this.me = me; this.chatApi = chatApi; this.infoApi = infoApi;
//         peerList.setItems(FXCollections.observableArrayList(me.getChats()
//             .stream().flatMap(c -> c.getUsers().stream())
//             .filter(u -> !u.getId().equals(me.getId()))
//             .collect(Collectors.toSet())));
//         peerList.getSelectionModel().selectedItemProperty().addListener((obs,_,peer) -> loadChat(peer));
//     }

//     private void loadChat(User peer) {
//         // find or create Chat between me and peer
//         chatApi.getOrCreateChat(List.of(me.getId(), peer.getId()))
//                .thenApply(Protocol::unwrapEntityResponse, Chat.class)
//                .thenAccept(chat -> Platform.runLater(() -> {
//                    currentChat = chat;
//                    messageList.setItems(FXCollections.observableArrayList(chat.getChatInfos()));
//                }));
//     }

//     @FXML private void onSend() {
//         String text = msgField.getText();
//         if (currentChat == null || text.isBlank()) return;
//         infoApi.createChatInfo(currentChat.getId(), me.getId(), text)
//                .thenApply(Protocol::unwrapEntityResponse, ChatInfo.class)
//                .thenAccept(msg -> Platform.runLater(() -> {
//                    messageList.getItems().add(msg);
//                    msgField.clear();
//                }));
//     }
// }
