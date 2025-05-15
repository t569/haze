package com.example.clientfx.controller;


import com.example.clientfx.api.UserApi;
import com.example.clientfx.api.ApiClient;
import com.example.clientfx.network.FxProtoClient;
import com.example.model.User;
import com.example.socket.server.Protocol;
import com.example.Config;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.mapping.Table;

public class UserListController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, Long> idColumn;
    @FXML private Button refreshButton;
    @FXML private Button deleteButton;
    @FXML private Button ediButton;
    @FXML private Button newButton;


    private UserApi userApi;
    private ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    public void initialize(){

        // Wire table columns
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<Long>(cell.getValue().getId()));
        usernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));

        userTable.setItems(users);
        userTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Setup API client
        try {

            // TODO: change the name for client
            FxProtoClient fxProtoClient = new FxProtoClient(Config.SERVER_NAME,Config.PORT_NUMBER, "client");
            ApiClient apiClient = new ApiClient(fxProtoClient);
            userApi = new UserApi(apiClient);
        }
        catch (IOException e) {
            showAlert("Connection Error", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        // load initial data
        // onLoadUsers();

        // Button actions
        // refreshButton.setOnAction(event -> onLoadUsers());
        // deleteButton.setOnAction(event -> onDeleteUser());
        newButton.setOnAction(event -> onCreateUser());
        ediButton.setOnAction(event -> onEditUser());
    }

    // TODO: implement these methods and make it work with the current API
    // private void onLoadUsers() {
    //     CompletableFuture<Protocol> future = userApi.getAllUsers();
    //     future.thenAccept(response -> {
    //         if (response.getStatus() == Protocol.Status.CONN_CONF) {
    //             List<User> userList = response.getPacket().getMetaData().getPayload().get();
    //             Platform.runLater(() -> {
    //                 users.clear();
    //                 users.addAll(userList);
    //             });
    //         } else {
    //             showAlert("Error", "Failed to load users", Alert.AlertType.ERROR);
    //         }
    //     });
    // }

    
    // TODO: implement these methods and make it work with the current API
    // private void onDeleteUser() {
    //     User selectedUser = userTable.getSelectionModel().getSelectedItem();
    //     if (selectedUser != null) {
    //         CompletableFuture<Protocol> future = userApi.deleteUser(selectedUser.getId());
    //         future.thenAccept(response -> {
    //             if (response.getStatus() == Protocol.Status.CONN_CONF) {
    //                 Platform.runLater(() -> onLoadUsers());
    //             } else {
    //                 showAlert("Error", "Failed to delete user", Alert.AlertType.ERROR);
    //             }
    //         });
    //     } else {
    //         showAlert("Error", "No user selected", Alert.AlertType.WARNING);
    //     }
    // }

    // TODO: Actually talk to the backend
    private void onCreateUser() {
         try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user_form.fxml"));
        Parent root = loader.load();
        UserFormController controller = loader.getController();
        controller.init(userApi.getApi(), null, user -> {

            // Send to the backend
            userApi.createUser(user.getName()).thenAccept(response -> {
                if (response.getStatus() == Protocol.Status.CONN_CONF) {

                    // check if the response was not null
                    if(response.getPacket().getMetaData().getPayload().isPresent())
                    {
                        User savedUser = (User) response.getPacket().getMetaData().getPayload().get();

                        // Update the UI
                        Platform.runLater(() -> {
                        users.add(savedUser);
                        userTable.refresh();
                    });
                    }
                
                } else {
                    Platform.runLater(() -> {
                        // Handle error
                        showAlert("Error", "Failed to create user", Alert.AlertType.ERROR);
                    });
                }
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    showAlert("Error", "Server error: " + ex.getMessage(), Alert.AlertType.ERROR);
                });
                return null;
            });
        });

        // Update the UI
        Stage stage = new Stage();
        stage.setTitle("Create New User");
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(userTable.getScene().getWindow());
        stage.showAndWait();
    } catch (IOException e) {
        showAlert("Error", "Unable to load user form: " + e.getMessage(), Alert.AlertType.ERROR);
    }
    }


    private void onEditUser() {
       User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a user to edit.", Alert.AlertType.WARNING);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user_form.fxml"));
            Parent root = loader.load();
            UserFormController controller = loader.getController();

            controller.init(userApi.getApi(), selected, updatedUser -> {
                // Step 1: Update user on the backend
                userApi.updateUser(updatedUser).thenAccept(response -> {
                    if (response.getStatus() == Protocol.Status.CONN_CONF) {
                        Platform.runLater(() -> {
                            userTable.refresh();
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert("Error", "Failed to update user on server.", Alert.AlertType.ERROR);
                        });
                    }
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showAlert("Error", "Server error: " + ex.getMessage(), Alert.AlertType.ERROR);
                    });
                    return null;
                });
            });

            // Update the UI
            Stage stage = new Stage();
            stage.setTitle("Edit User");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(userTable.getScene().getWindow());
            stage.showAndWait();
        } catch (IOException e) {
            showAlert("Error", "Unable to load user form: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

     private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
