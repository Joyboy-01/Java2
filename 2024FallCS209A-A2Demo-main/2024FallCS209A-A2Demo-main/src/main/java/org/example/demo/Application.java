package org.example.demo;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

import static org.example.demo.Game.SetupBoard;

public class Application extends javafx.application.Application {
    private Client client;
    private StackPane rootPane;
    private Text matchingText;
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize client and connect to server
        int[] size = getBoardSizeFromUser();
        client = new Client("localhost", 1234);
        client.setOnBoardReceived(this::onBoardReceived); // Set callback for when board is received from server
        client.start();

        int[][] initialBoard = SetupBoard(size[0], size[1]);
        client.sendInitialBoard(initialBoard);

        rootPane = new StackPane();
        matchingText = new Text("Waiting for another player to join...");
        rootPane.getChildren().add(matchingText);

        Scene scene = new Scene(rootPane,500,500);
        stage.setTitle("Matching");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            System.out.println("Application is closing...");
            Platform.exit();
            System.exit(0);
        });
        stage.show();
    }

    private void onBoardReceived(int[][] initialBoard) {
        System.out.println("Board received, updating UI...");
        Platform.runLater(() -> {
            try {
                rootPane.getChildren().clear();
                Controller.game = new Game(initialBoard);
                FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("board.fxml"));
                VBox gameRoot = fxmlLoader.load();
                Controller controller = fxmlLoader.getController();
                controller.initialize(client);
                client.setController(controller);
                controller.createGameBoard();
                rootPane.getChildren().add(gameRoot);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void showLoginScreen(Stage stage) throws IOException {
        // Load the login FXML and controller
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("login.fxml"));
        StackPane loginRoot = fxmlLoader.load();
        LoginController loginController = fxmlLoader.getController();

        // Set the client and login success callback in the controller
        loginController.setClient(client);
        loginController.setOnLoginSuccess(() -> {
            // Once logged in, transition to the game screen
            try {
                showGameScreen(stage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Set the login screen scene
        Scene loginScene = new Scene(loginRoot, 400, 300);
        stage.setTitle("Login");
        stage.setScene(loginScene);
        stage.show();
    }

    private void showGameScreen(Stage stage) throws IOException {
        int[] size = getBoardSizeFromUser();
        client = new Client("localhost", 1234);

        client.setOnBoardReceived(this::onBoardReceived); // Set callback for when board is received from server
        client.start();

        int[][] initialBoard = SetupBoard(size[0], size[1]);
        client.sendInitialBoard(initialBoard);

        rootPane = new StackPane();
        matchingText = new Text("Waiting for another player to join...");
        rootPane.getChildren().add(matchingText);

        Scene scene = new Scene(rootPane,500,500);
        stage.setTitle("Matching");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            System.out.println("Application is closing...");
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }
//    @Override
//    public void start(Stage stage) throws IOException {
//
//        StackPane rootPane = new StackPane();
//
//        // 添加初始等待文本到 rootPane
//        Text waitingText = new Text("等待连接到服务器...");
//        rootPane.getChildren().add(waitingText);
//
//        // 创建并显示初始场景
//        Scene scene = new Scene(rootPane, 400, 300);
//        stage.setTitle("游戏启动中");
//        stage.setScene(scene);
//        stage.show();
//        client = new Client("localhost", 1234);
//        client.start();
//
//        int[] size = getBoardSizeFromUser();
//        int[][] initialBoard = SetupBoard(size[0], size[1]);
//        Controller.game = new Game(initialBoard);
//
//        client.sendInitialBoard(initialBoard);
//
//        rootPane.getChildren().clear();
//        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("board.fxml"));
//        VBox root = fxmlLoader.load();
//        Controller controller = fxmlLoader.getController();
//        controller.initialize(client);
//        controller.createGameBoard();
//
//        rootPane.getChildren().add(root);
//    }
//
//
//        Scene scene = new Scene(root);
//        stage.setTitle("Hello!");
//        stage.setScene(scene);
//        stage.show();
//
//        // TODO: handle the game logic
//    }
    // let user choose board size
    protected int[] getBoardSizeFromUser() {
        // TODO: let user choose board size
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Board Size");
        dialog.setHeaderText("Choose Board Size");
        dialog.setContentText("Enter rows and columns (e.g., 4x4):");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String[] size = result.get().split("x");
            return new int[]{Integer.parseInt(size[0]), Integer.parseInt(size[1])};
        }
        return new int[]{4, 4}; // Default size
    }

    public static void main(String[] args) {
        launch();
    }
}