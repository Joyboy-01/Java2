package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OnBoardReceivedListener onBoardReceivedListener;
    private Controller controller;
    public boolean isMyTurn = false;
    private boolean loginResult = false;
    private boolean registerResult = false;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean responseReceived = false;
    private DashboardController dashboardController;

    public Client(String host, int port) {
        try {
            this.socket = new Socket(host, port);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            isMyTurn = false;
            System.out.println("Connected to server on port: " + socket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }
    public void start() {
        new Thread(this::listenToServer).start();
        System.out.println("Client started and listening to server...");
        //sendMessage("JOIN");
    }

    public void sendInitialBoard(int[][] board) {
        System.out.println("Sending initial board to server");
        sendMessage("INIT_BOARD " + boardToString(board));
    }

    private void listenToServer() {
        String message;
        try {
            while ((message = in.readLine()) != null) {
                if (message.startsWith("INIT_BOARD")) {
                    int[][] board = parseBoard(message);
                    if (onBoardReceivedListener != null) {
                        onBoardReceivedListener.onBoardReceived(board);
                    }
                }
                if (message.startsWith("VALID_MOVE")) {
                    String[] parts = message.split(" ");
                    int row1 = Integer.parseInt(parts[1]);
                    int col1 = Integer.parseInt(parts[2]);
                    int row2 = Integer.parseInt(parts[3]);
                    int col2 = Integer.parseInt(parts[4]);
                    Platform.runLater(() -> controller.handleValidMove(row1, col1, row2, col2));
                }
                if (message.startsWith("INVALID_MOVE")) {
                    Platform.runLater(() -> showError("INVALID_MOVE,NO SCORE"));
                }
                if (message.startsWith("SCORE")) {
                    String finalMessage = message;
                    Platform.runLater(() -> {
                        controller.scoreLabel.setText(finalMessage.split(" ")[1]);
                    });
                }
                if (message.startsWith("MATCH")) {
                    Platform.runLater(this::showCountdownWindow);
                }
                if (message.startsWith("YOUR_TURN")) {
                    isMyTurn = true;
                    Platform.runLater(() -> {
                        controller.isMyturnLabel.setText("Your Turn");
                        System.out.println("It's your turn");
                    });
                }
                if (message.equals("WAIT")) {
                    isMyTurn = false;
                    Platform.runLater(() -> {
                        controller.isMyturnLabel.setText("Waiting for opponent...");
                        System.out.println("Waiting for opponent");
                    });
                }
                if (message.startsWith("BOARD")) {
                    int[][] board = parseBoard(message);
                    if (onBoardReceivedListener != null) {
                        onBoardReceivedListener.onBoardReceived(board);
                    }
                }
                if (message.startsWith("MATCH")) {
                    Platform.runLater(this::showCountdownWindow);
                }
                if (message.equals("GAME_OVER WIN")) {
                    isMyTurn = false;
                    Platform.runLater(() -> {
                        controller.gameStatusLabel.setText("you win");
                        showGameOver("YOU WIN");
                    });
                }
                if (message.equals("GAME_OVER LOSE")) {
                    isMyTurn = false;
                    Platform.runLater(() -> {
                        controller.gameStatusLabel.setText("you lose");
                        showGameOver("YOU LOSE");
                    });
                }
                if (message.equals("GAME_OVER TIE")) {
                    isMyTurn = false;
                    Platform.runLater(() -> {
                        controller.gameStatusLabel.setText("tie");
                        showGameOver("TIE");
                    });
                }
                if (message.equals("opponent disconnected")) {
                    isMyTurn = false;
                    Platform.runLater(() -> {
                        controller.gameStatusLabel.setText("opponent disconnected");
                        showGameOver("opponent disconnected");
                    });
                    System.out.println("opponent disconnected");
                }
                if (message.equals("LOGIN_SUCCESS")) {
                    lock.lock();
                    try {
                        loginResult = true;
                        responseReceived = true;
                        System.out.println("LOGIN_SUCCESS");
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
                if (message.equals("LOGIN_FAILED")) {
                    lock.lock();
                    try {
                        loginResult = false;
                        responseReceived = true;
                        System.out.println("LOGIN_FAILED");
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
                if (message.equals("REGISTER_SUCCESS")) {
                    lock.lock();
                    try {
                        registerResult = true;
                        responseReceived = true;
                        System.out.println("REGISTER_SUCCESS");
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
                if (message.equals("REGISTER_FAILED")) {
                    lock.lock();
                    try {
                        registerResult = false;
                        responseReceived = true;
                        System.out.println("REGISTER_FAILED");
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }
                if (message.startsWith("HISTORY")) {
                    System.out.println("receive HISTORY");
                    if (dashboardController != null) {
                        dashboardController.updateInfo("Game History:\n" + message.substring(9));
                    }
                }
                if (message.startsWith("STATUS")) {
                    System.out.println("receive STATUS");
                    if (dashboardController != null) {
                        dashboardController.updateInfo("User Status:\n" + message.substring(8));
                    }
                }
            }
        } catch (IOException e) {
            Platform.runLater(() -> {
                showAlert("服务器断开连接", "disconnected");
            });
//            reconnectToServer();
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ERROR");
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType okButton = new ButtonType("YES", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.showAndWait();
    }

    private void showGameOver(String resultMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("GAME OVER");
        alert.setHeaderText(null);
        alert.setContentText(resultMessage);
        ButtonType okButton = new ButtonType("YES", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.showAndWait();
    }

    private void showCountdownWindow() {
        Stage countdownStage = new Stage();
        StackPane root = new StackPane();
        Label countdownLabel = new Label("MATCH success,Game starts in 3...");
        root.getChildren().add(countdownLabel);

        Scene scene = new Scene(root, 200, 100);
        countdownStage.setScene(scene);
        countdownStage.setTitle("Game Starting");

        countdownStage.initModality(Modality.APPLICATION_MODAL);

        Timeline countdownTimeline = getTimeline(countdownLabel, countdownStage);
        countdownTimeline.play();
        countdownStage.showAndWait();
    }

    private static Timeline getTimeline(Label countdownLabel, Stage countdownStage) {
        AtomicInteger remainingTime = new AtomicInteger(3);
        Timeline countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String text = countdownLabel.getText();

                    remainingTime.getAndDecrement();
                    if (remainingTime.get() > 0) {
                        countdownLabel.setText("MATCH success,Game starts in " + remainingTime + "...");
                    } else {
                        countdownLabel.setText("Go!");
                        countdownStage.close();
//                        countdownLabel.setText("MATCH success,Game starts in " + remainingTime + "...");
                    }
                })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        return countdownTimeline;
    }

    private int[][] parseBoard(String message) {
        String[] rows = message.replace("BOARD ", "").split(";");
        int[][] board = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            board[i] = Arrays.stream(rows[i].trim().split(" "))
                    .mapToInt(Integer::parseInt).toArray();
        }
        return board;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setOnBoardReceived(OnBoardReceivedListener listener) {
        this.onBoardReceivedListener = listener;
    }

    public void notifyServerUserOffline() {
        sendMessage("USER_OFFLINE ");
    }

    public interface OnBoardReceivedListener {
        void onBoardReceived(int[][] board);
    }

    private String boardToString(int[][] board) {
        StringBuilder boardStr = new StringBuilder();
        for (int[] row : board) {
            for (int cell : row) {
                boardStr.append(cell).append(" ");
            }
            boardStr.append(";");
        }
        return boardStr.toString();
    }
    private void reconnectToServer() {
        new Thread(() -> {
            int retryCount = 0; // 初始化重试次数
            int maxRetries = 5; // 设置最大重试次数

            while (retryCount < maxRetries) {
                try {
                    retryCount++;
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    Platform.runLater(() -> {
                        showAlert("重新连接成功", "您已重新连接到服务器。");
                    });

                    break;

                } catch (IOException e) {
                    System.err.println("尝试重新连接失败，第 " + retryCount + " 次");

                    if (retryCount >= maxRetries) {
                        Platform.runLater(() -> {
                            showAlert("连接失败", "无法重新连接到服务器，程序将退出。");
                        });
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {}

                        System.exit(0);
                    }

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public boolean login(String username, String password) {
        String loginMessage = "LOGIN " + username + " " + password;
        sendMessage(loginMessage);
        System.out.println("logging");
        lock.lock();
        try {
            responseReceived = false;
            while (!responseReceived) {
                condition.await();  // 等待服务器的响应
            }
            return loginResult;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean register(String username, String password) {
        String registerMessage = "REGISTER " + username + " " + password;
        sendMessage(registerMessage);
        System.out.println("registering");
        lock.lock();
        try {
            responseReceived = false;
            System.out.println(1);
            while (!responseReceived) {
                System.out.println(2);
                condition.await();  // 等待服务器的响应
            }
            System.out.println(3);
            return registerResult;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }
}
