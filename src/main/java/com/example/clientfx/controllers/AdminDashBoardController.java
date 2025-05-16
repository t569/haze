package com.example.clientfx.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import com.example.clientfx.api.ApiClient;
import com.example.clientfx.api.UserApi;
import com.example.model.User;
import com.example.socket.server.Protocol;

// here's the idea
// this has a dashboard to create the new Users, 
// list the Users

// Under each listed user, onclick(select) on them will lead you to a new window
// that window is for you to Delete User or Edit User or Login as User

// This is for the spawned new Chat window for a particular user
// If Login as User, provide a list of all the available chats for user in a new Chat window
// the window has a side bar that has the other chats aswell as the current chat being viewed
// in this chat window, you can send messages delete messages, and edit messages



public class AdminDashBoardController {

    // Reference to the TableView in the FXML
    @FXML
    private TableView<User> userTable;

    // Table columns for displaying user fields
    @FXML
    private TableColumn<User, Long> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    // The input fields and buttons for a new user
    @FXML
    private TextField usernameField;
    @FXML
    private Button createButton;

    // API client for user operations
    private UserApi userApi;

    // backing list for the TableView
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    public void init(ApiClient apiClient)
    {
        this.userApi = new UserApi(apiClient);

         // load users from the API
        loadUsers();
    }

    /* 
     * Called by FXMLLoader when initialisation is complete
     * Set up table columns and loads the initial user list
     */
    @FXML
    private void initialize()
    {
        // map model properties to table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));


        // bind the backing list to the TableView
        userTable.setItems(userList);

         // Set placeholder when there's no data
        userTable.setPlaceholder(new Label("No users found"));

        // wire the create button 
        createButton.setOnAction(this::handleCreateUser);

    }

    // fetch all the users from the server and populate the table
    private void loadUsers()
    // Send the GET_ALL request
    {        
        userApi.getAllUsers().thenAccept(response -> {
            if (response.getPacket().getMetaData().getCommProtocol().get() == Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK) {
                Optional<Object> payload = response.getPacket().getMetaData().getPayload();
                if (payload.isPresent()) {
                    @SuppressWarnings("unchecked")
                    List<User> users = (List<User>) payload.get();
                    Platform.runLater(() -> {
                        userList.setAll(users);
                    });
                }
            } else {
                showAlert("Error", "Failed to load users", Alert.AlertType.ERROR);
            }
        }).exceptionally(ex -> {
            Platform.runLater(() -> showAlert("Error", ex.getMessage(), Alert.AlertType.ERROR));
            return null;
        });
    }


    // handle for create button. read input fields, calls API, and refreshes table
    @FXML
    private void handleCreateUser(ActionEvent event)
    {
        // Read all the fields
        String username = usernameField.getText();

        // Basic validation
        // TODO: make it into a new function

        if(username.isBlank())
        {
            System.err.println("All fields must be filled out");
            return;
        }
        else 
        {
            // Construct the user
            try 
            {
                System.out.println("Hi "+ username + "!");
                // userApi.createUser(username);

                // // clear input fields upon success
                // usernameField.clear();

                // reload the user list to show newly added user
                // loadUsers();

                // Send to the backend
                userApi.createUser(username).thenAccept(response -> {
                    if(response.getStatus() == Protocol.Status.CONN_CONF)
                    {
                        // check if the response was null i.e we created the user
                        if(response.getPacket().getMetaData().getPayload().isPresent())
                        {
                            // User savedUser = (User) response.getPacket().getMetaData().getPayload().get();

                            // Update the UI
                            Platform.runLater(() ->{
                                loadUsers();
                            });
                        }
                        else
                        {
                            Platform.runLater(() ->
                            {
                                // Handle error
                                showAlert("Error", "failed to create user", Alert.AlertType.ERROR);
                            });
                        }
                    }
                }).exceptionally(ex -> {
                    // Handle error
                    Platform.runLater(() -> {
                        showAlert("Error", "Server error" + ex.getMessage(), Alert.AlertType.ERROR);
                    });
                    return null;
                });
            }
            catch(Exception e)
            {
                System.err.println("Failed to create user: "+ e.getMessage());
            }
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
