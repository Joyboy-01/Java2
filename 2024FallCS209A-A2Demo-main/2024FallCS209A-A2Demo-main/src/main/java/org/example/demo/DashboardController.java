package org.example.demo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DashboardController {
    private Client client;

    @FXML
    private Button viewHistoryButton;

    @FXML
    private Button viewUserStatusButton;

    @FXML
    private TextArea infoTextArea;

    @FXML
    private Button joinGameButton;

    private Application application;
    public void initialize() {
        viewHistoryButton.setOnAction(event -> viewHistory());
        viewUserStatusButton.setOnAction(event -> viewUserStatus());
        joinGameButton.setOnAction(event -> joinGame());
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    private void viewHistory() {
        if (client != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("View User HISTORY");
            dialog.setHeaderText("Enter username to view history:");
            dialog.setContentText("Username:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(username -> {
                client.sendMessage("HISTORY " + username);
            });
        }
    }

    private void viewUserStatus() {
//        if (client != null) {
//            TextInputDialog dialog = new TextInputDialog();
//            dialog.setTitle("View User Status");
//            dialog.setHeaderText("Enter username to view status:");
//            dialog.setContentText("Username:");
//
//            Optional<String> result = dialog.showAndWait();
//            result.ifPresent(username -> {
//                client.sendMessage("STATUS " + username);
//            });
//        }
        client.sendMessage("STATUS");
    }

//    private void joinGame() {
//        if (client != null) {
//            //client.sendMessage("JOIN");
//            if (application != null) {
//                try {
//                    client.sendMessage("JOIN");
//                    application.showGameScreen((Stage) joinGameButton.getScene().getWindow());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
    private void joinGame() {
        if (client != null) {

            client.sendMessage("GET_QUEUE");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<String> queueList = client.getQueue();

            if (queueList.isEmpty()) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Join Game");
                dialog.setHeaderText("No opponents available, join the queue");
                dialog.setContentText("Enter 'JOIN' to join the queue:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(choice -> {
                    if (choice.equalsIgnoreCase("JOIN")) {
                        startGame(() -> {
                            client.sendMessage("JOIN");
                            infoTextArea.setText("Waiting to be selected by another player...");
                        });
                    }
                });
            } else {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Challenge Opponent");
                dialog.setHeaderText("Select an opponent from the list below to challenge:\n" + String.join(", ", queueList));
                dialog.setContentText("Enter opponent's username to challenge or enter 'JOIN' to join the queue:");

                Optional<String> result = dialog.showAndWait();
                result.ifPresent(choice -> {
                    if (choice.equalsIgnoreCase("JOIN")) {
                        startGame(() -> {
                            client.sendMessage("JOIN");
                            infoTextArea.setText("Waiting to be selected by another player...");
                        });
                    } else {
                        String opponentUsername = choice.trim();
                        startGame(() -> {
                            client.sendMessage("CHALLENGE " + opponentUsername);
                            infoTextArea.setText("Challenging " + opponentUsername + "...");
                        });
                    }
                });
            }
        }
    }

    public void startGame(Runnable onGameReady) {
        Platform.runLater(() -> {
            try {
                Stage currentStage = (Stage) joinGameButton.getScene().getWindow();
                application.showGameScreen(currentStage);
                onGameReady.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void updateInfo(String info) {
        infoTextArea.appendText(info + "\n");
    }
}
