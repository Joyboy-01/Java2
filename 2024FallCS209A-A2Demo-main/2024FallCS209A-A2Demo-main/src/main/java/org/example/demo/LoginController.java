package org.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    private Client client;
    private Runnable onLoginSuccess;
    private Runnable onReconnectSuccess;

    public void setClient(Client client) {
        this.client = client;
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void setOnReconnectSuccess(Runnable onReconnectSuccess) {
        this.onReconnectSuccess = onReconnectSuccess;
    }

    @FXML
    public void initialize() {
        loginButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (client != null && client.reconnect(username, password)) {
                System.out.println("reconnect successful");
                if (onReconnectSuccess != null) {
                    onReconnectSuccess.run();
                }
            }else if (client != null && client.login(username, password)) {
                System.out.println("Login successful");
                if (onLoginSuccess != null) {
                    onLoginSuccess.run();
                }
            } else {
                showAlert("Login failed", "Invalid username or password.");
            }
        });

        registerButton.setOnAction(event -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            if (client != null && client.register(username, password)) {
                showAlert("Registration successful", "You can now log in.");
            } else {
                showAlert("Registration failed", "Username already exists.");
            }
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
