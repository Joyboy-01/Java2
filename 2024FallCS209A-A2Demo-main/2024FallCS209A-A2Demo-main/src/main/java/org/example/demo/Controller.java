package org.example.demo;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import java.util.List;
import java.util.Objects;

public class Controller {
    @FXML
    public Label gameStatusLabel;
    @FXML
    Label scoreLabel;
    @FXML
    Label isMyturnLabel;
    @FXML
    private GridPane gameBoard;
    @FXML
    private Pane linePane;
    public static Game game;
    //private int[] selectedPosition = new int[2];
    int[] position = new int[3];
    public int[][] originalBoard = copyBoard(game.board);

    private Client client; // 客户端对象，用于发送消息到服务器
    private int[][] copyBoard(int[][] board) {
        int[][] newBoard = new int[board.length][];
        for (int i = 0; i < board.length; i++) {
            newBoard[i] = board[i].clone();
        }
        return newBoard;
    }

    @FXML
    public void initialize(Client client) {
        this.client = client;
        if (linePane != null) {
            linePane.getChildren().clear();
        }
    }

    public void createGameBoard() {
        gameBoard.getChildren().clear();
        for (int row = 0; row < game.row; row++) {
            for (int col = 0; col < game.col; col++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                ImageView imageView = addContent(game.board[row][col]);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                button.setGraphic(imageView);
                int finalRow = row;
                int finalCol = col;
                button.setOnAction( _ -> handleButtonPress(finalRow, finalCol));
                gameBoard.add(button, col, row);
            }
        }
    }
//    private void handleButtonPress(int row, int col) {
//        System.out.println("Button pressed at: " + row + ", " + col);
//
//        if (client != null) {
//            client.sendMessage("SELECT " + row + " " + col);
//        }
//        if (selectedPosition[0] == -1) {
//            selectedPosition[0] = row;
//            selectedPosition[1] = col;
//        } else {
//            client.sendMessage("MOVE " + selectedPosition[0] + " " + selectedPosition[1] + " " + row + " " + col);
//            selectedPosition[0] = -1;
//        }
//    }
    private void handleButtonPress(int row, int col) {
        if (client.isMyTurn){
            System.out.println("Button pressed at: " + row + ", " + col);
            if(position[0] == 0){
                position[1] = row;
                position[2] = col;
                position[0] = 1;
            }else{
                position[0] = 0;
                List<int[]> path = game.getPath(position[1], position[2], row, col);
                if (!path.isEmpty()) {
                    drawPath(path);
                    handleValidMove(position[1], position[2], row, col);
                }
                if (client != null) {
                    System.out.println("send to server"+" MOVE " + row + " " + col+" "+position[1]+" "+position[2]);
                    client.sendMessage("MOVE " + row + " " + col+" "+position[1]+" "+position[2]); // 将用户的操作上传给服务器
                }
//            boolean change = game.judge(position[1], position[2], row, col);
//            position[0] = 0;
//            if(change){
//                // TODO: handle the grid deletion logic
//                game.board[position[1]][position[2]] = 0;
//                game.board[row][col] = 0;
//                Button button1 = (Button) gameBoard.getChildren().get(position[1] * game.col + position[2]);
//                Button button2 = (Button) gameBoard.getChildren().get(row * game.col + col);
//                button1.setGraphic(null);
//                button2.setGraphic(null);
//                button1.setDisable(true);
//                button2.setDisable(true);
//                updateScore();
//                if (checkGameOver())showGameOver();
//            }else{
//                showError("这两个图标不能连接，请尝试其他图标。");
//            }
            }
        }
    }
    public void handleValidMove(int row1, int col1, int row2, int col2) {
        System.out.println("VALID_MOVD FROM (" + row1 + ", " + col1 + ") TO (" + row2 + ", " + col2 + ")");
        game.board[row1][col1] = 0;
        game.board[row2][col2] = 0;
        Button button1 = (Button) gameBoard.getChildren().get(row1 * game.col + col1);
        Button button2 = (Button) gameBoard.getChildren().get(row2 * game.col + col2);
        button1.setGraphic(null);
        button2.setGraphic(null);
        button1.setDisable(true);
        button2.setDisable(true);
//        updateScore();
//        if (checkGameOver())showGameOver();

    }
//    private int score = 0;
//    private void updateScore() {
//        client.setScore(client.getScore() + 1);
//        Platform.runLater(() -> {
//            scoreLabel.setText(String.valueOf(client.getScore()));
//        });
//    }

    @FXML
    void handleReset() {
        position[0] = 0;
        game.board = copyBoard(originalBoard);
        gameBoard.getChildren().clear();
        createGameBoard();
    }

    public ImageView addContent(int content){
        return switch (content) {
            case 0 -> new ImageView(imageCarambola);
            case 1 -> new ImageView(imageApple);
            case 2 -> new ImageView(imageMango);
            case 3 -> new ImageView(imageBlueberry);
            case 4 -> new ImageView(imageCherry);
            case 5 -> new ImageView(imageGrape);
            case 6 -> new ImageView(imageKiwi);
            case 7 -> new ImageView(imageOrange);
            case 8 -> new ImageView(imagePeach);
            case 9 -> new ImageView(imagePear);
            case 10 -> new ImageView(imagePineapple);
            case 11 -> new ImageView(imageWatermelon);
            default -> null;
        };
    }

    private void drawPath(List<int[]> path) {
        linePane.getChildren().clear(); // 每次绘制前清空连线

        // 绘制新连线
        for (int i = 0; i < path.size() - 1; i++) {
            int[] start = path.get(i);
            int[] end = path.get(i + 1);

            Bounds startBounds = getButtonBounds(start[0], start[1]);
            Bounds endBounds = getButtonBounds(end[0], end[1]);

            double startX = startBounds.getMinX() + startBounds.getWidth() / 2;
            double startY = startBounds.getMinY() + startBounds.getHeight() / 2;
            double endX = endBounds.getMinX() + endBounds.getWidth() / 2;
            double endY = endBounds.getMinY() + endBounds.getHeight() / 2;

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(Color.RED);
            line.setStrokeWidth(3);
            linePane.getChildren().add(line);
        }

        // 延迟移除连线
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(_ -> linePane.getChildren().clear());
        pause.play();
    }

    private Bounds getButtonBounds(int row, int col) {
        Button button = (Button) gameBoard.getChildren().get(row * game.col + col);
        return button.localToParent(button.getBoundsInLocal());
    }

//    private void showGameOver() {
//
//        Dialog<Void> dialog = new Dialog<>();
//        dialog.setTitle("GAME OVER");
//        dialog.setHeaderText("END");
//        dialog.setContentText("THANKS FOR PLAYING");
//
//        ButtonType okButton = new ButtonType("YES", ButtonBar.ButtonData.OK_DONE);
//        dialog.getDialogPane().getButtonTypes().add(okButton);
//
//        dialog.showAndWait(); // 显示对话框，等待用户点击“确定”
//    }
//    private void showError(String message) {
//        Dialog<Void> dialog = new Dialog<>();
//        dialog.setTitle("ERROR");
//        dialog.setHeaderText("ERROR");
//        dialog.setContentText(message);
//
//        ButtonType okButton = new ButtonType("YES", ButtonBar.ButtonData.OK_DONE);
//        dialog.getDialogPane().getButtonTypes().add(okButton);
//        dialog.show();
//    }

    public static Image imageApple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/apple.png")).toExternalForm());
    public static Image imageMango = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/mango.png")).toExternalForm());
    public static Image imageBlueberry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/blueberry.png")).toExternalForm());
    public static Image imageCherry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/cherry.png")).toExternalForm());
    public static Image imageGrape = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/grape.png")).toExternalForm());
    public static Image imageCarambola = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/carambola.png")).toExternalForm());
    public static Image imageKiwi = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/kiwi.png")).toExternalForm());
    public static Image imageOrange = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/orange.png")).toExternalForm());
    public static Image imagePeach = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/peach.png")).toExternalForm());
    public static Image imagePear = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pear.png")).toExternalForm());
    public static Image imagePineapple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pineapple.png")).toExternalForm());
    public static Image imageWatermelon = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/watermelon.png")).toExternalForm());

}
