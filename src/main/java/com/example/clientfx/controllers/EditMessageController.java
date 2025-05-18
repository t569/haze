package com.example.clientfx.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.clientfx.api.ChatInfoApi;
import com.example.model.ChatInfo;
import com.example.socket.server.Protocol;

import java.util.function.Consumer;

public class EditMessageController {
    @FXML private TextField messageField;
    @FXML private Button saveButton;

    private ChatInfoApi chatInfoApi;
    private ChatInfo original;
    private Consumer<ChatInfo> onSaved;

    /**
     * Initialize with the ChatInfo to edit, api, and callback.
     */
    public void init(ChatInfo original, ChatInfoApi chatInfoApi, Consumer<ChatInfo> onSaved) {
        this.original = original;
        this.chatInfoApi = chatInfoApi;
        this.onSaved = onSaved;

        messageField.setText(original.getText());
        saveButton.setOnAction(evt -> saveEdits());
    }

    private void saveEdits() {
        String newText = messageField.getText().trim();
        if (newText.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Message cannot be empty.", ButtonType.OK)
                .showAndWait();
            return;
        }

        // Apply change
        original.setText(newText);
        chatInfoApi.updateChatInfo(original).thenAccept(response -> {
            if (response.getPacket().getMetaData().getCommProtocol().orElse(null)
                    == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                Platform.runLater(() -> {
                    onSaved.accept(original);
                    close();
                });
            } else {
                Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR, "Failed to update message.", ButtonType.OK)
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
        close();
    }

    private void close() {
        Stage stage = (Stage) messageField.getScene().getWindow();
        stage.close();
    }
}
