package org.example.demo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GameSession extends Thread {
    private static final Logger logger = Logger.getLogger(GameSession.class.getName());
    private final ClientHandler player1;
    private final ClientHandler player2;
    private final Game game;
    private boolean player1Turn;
    private final Lock lock = new ReentrantLock();
    private final Condition moveMade = lock.newCondition();
    private int player1Score = 0;
    private int player2Score = 0;
    private volatile boolean moveProcessed = false;
    private volatile boolean sessionActive = true;

    static {
        try {
            FileHandler fileHandler = new FileHandler("2024FallCS209A-A2Demo-main/2024FallCS209A-A2Demo-main/src/main/java/org/example/demo/session.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);  // 不使用默认的控制台输出
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public GameSession(ClientHandler player1, ClientHandler player2, int[][] initialBoard) {
        this.player1 = player1;
        this.player2 = player2;
        this.game = new Game(initialBoard);
        this.player1Turn = new Random().nextBoolean();
        player1.setMoveListener(this::onMoveReceived);
        player2.setMoveListener(this::onMoveReceived);

        logger.info("Game session created between clients on ports: "
                + player1.getSocket().getPort() + " and " + player2.getSocket().getPort()
                + " at " + new Date());
    }

    @Override
    public void run() {
        sendInitialBoard();
        while (sessionActive && !isGameOver()) {
            ClientHandler currentPlayer = player1Turn ? player1 : player2;
            ClientHandler currentPlayer2 = player1Turn ? player2 : player1;
            currentPlayer.sendMessage("YOUR_TURN");
            currentPlayer2.sendMessage("WAIT");
            lock.lock();
            try {
                while (!moveProcessed) {
                    moveMade.await();
                }
                moveProcessed = false;
                player1Turn = !player1Turn;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
//            try {
//                String move = currentPlayer.receiveMove();
//                System.out.println("RECEIVE MOVE2");
//                lock.lock();
//                try {
//                    processMove(move, currentPlayer);
//                } finally {
//                    lock.unlock();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                break;
//            }
//            currentPlayer.setMoveListener(this::onMoveReceived);
//            player1Turn = !player1Turn;
        }
        endGame();
    }

    private void sendInitialBoard() {
        System.out.println("Game session started between clients on ports: " + player1.getSocket().getPort() + " and " + player2.getSocket().getPort());
        broadcastBoard();
    }

    private void processMove(String move, ClientHandler player) throws IOException {
        String[] parts = move.split(" ");
//        for (int i = 0; i < parts.length; i++) {
//            System.out.println(parts[i]);
//        }
        if (parts.length != 5 || !parts[0].equals("MOVE")) {
            System.out.println(1);
            player.sendMessage("INVALID_MOVE_FORMAT");
            return;
        }else {
            System.out.println("receive MOVE");
        }

        int row1 = Integer.parseInt(parts[1]);
        int col1 = Integer.parseInt(parts[2]);
        int row2 = Integer.parseInt(parts[3]);
        int col2 = Integer.parseInt(parts[4]);

        if (game.judge(row1, col1, row2, col2)) {
            updateBoard(row1, col1, row2, col2);
            if (player == player1) {
                player1Score++;
            } else {
                player2Score++;
            }
            String validMoveMessage = "VALID_MOVE " + row1 + " " + col1 + " " + row2 + " " + col2;
            player1.sendMessage(validMoveMessage);
            player2.sendMessage(validMoveMessage);
            broadcastScore();
        } else {
            player.sendMessage("INVALID_MOVE");
        }
    }

    private void updateBoard(int row1, int col1, int row2, int col2) {
        game.board[row1][col1] = 0;
        game.board[row2][col2] = 0;
    }

    private void broadcastBoard() {
        player1.sendBoard(game.board);//, player1Turn ? "YOUR_TURN" : "WAIT");
        player2.sendBoard(game.board);//, player1Turn ? "WAIT" : "YOUR_TURN");
    }

    private void broadcastScore() {
        player1.sendMessage("SCORE " + player1Score + " " + player2Score);
        player2.sendMessage("SCORE " + player2Score + " " + player1Score);
    }

    private boolean isGameOver() {
        for (int[] row : game.board) {
            for (int cell : row) {
                if (cell != 0) {
                    return false;
                }
            }
        }

        for (int row1 = 0; row1 < game.board.length; row1++) {
            for (int col1 = 0; col1 < game.board[row1].length; col1++) {
                if (game.board[row1][col1] != 0) {
                    for (int row2 = row1; row2 < game.board.length; row2++) {
                        for (int col2 = (row1 == row2) ? col1 + 1 : 0; col2 < game.board[row2].length; col2++) {
                            if (game.board[row2][col2] != 0 && game.board[row1][col1] == game.board[row2][col2]) {
                                if (game.judge(row1, col1, row2, col2)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private void endGame() {
        sessionActive = false;
        String result;
        if (player1Score > player2Score) {
            if (!player1.isDisconnected()) player1.sendMessage("GAME_OVER WIN");
            if (!player2.isDisconnected()) player2.sendMessage("GAME_OVER LOSE");
            result = player1.getUsername()+" wins";
        } else if (player2Score > player1Score) {
            if (!player1.isDisconnected()) player1.sendMessage("GAME_OVER LOSE");
            if (!player2.isDisconnected()) player2.sendMessage("GAME_OVER WIN");
            result = player2.getUsername()+" wins";
        } else {
            if (!player1.isDisconnected()) player1.sendMessage("GAME_OVER TIE");
            if (!player2.isDisconnected()) player2.sendMessage("GAME_OVER TIE");
            result = "tie";
        }
        saveGameResult(player1.getUsername(), player2.getUsername(), result);
        logger.info("Game session ended between ports " + player1.getSocket().getPort() + " and " + player2.getSocket().getPort() + " at " + new Date());
    }

    public void onMoveReceived(String move, ClientHandler player) {
        lock.lock();
        try {
            if ((player1Turn && player == player1) || (!player1Turn && player == player2)) {
                moveProcessed = true;
                moveMade.signal();
                processMove(move, player);
            } else {
                player.sendMessage("WAITING");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
//    public void handleClientDisconnect(ClientHandler disconnectedPlayer) {
//        if (disconnectedPlayer == player1) {
//            player2.sendMessage("opponent disconnected");
//            player2.sendMessage("GAME_OVER WIN");
//        } else if (disconnectedPlayer == player2) {
//            player1.sendMessage("opponent disconnected");
//            player1.sendMessage("GAME_OVER WIN");
//        }
//        closeSession();
//    }
    public void handleClientDisconnect(ClientHandler disconnectedPlayer) {
        lock.lock();
        try {
            logger.warning("Client disconnected from port: "
                    + disconnectedPlayer.getSocket().getPort() + " at " + new Date());

            sessionActive = false;
            if (disconnectedPlayer == player1) {
                if (!player2.isDisconnected()) {
                    player2.sendMessage("opponent disconnected");
                    player2.sendMessage("GAME_OVER WIN");
                    logger.info("Client on port " + player2.getSocket().getPort() + " notified of opponent disconnection.");
                    saveGameResult(player1.getUsername(), player2.getUsername(), player2.getUsername()+" wins because opponent disconnected");
                } else {
                    logger.info("Both players disconnected. Ending session.");
                    saveGameResult(player1.getUsername(), player2.getUsername(), "nobody wins because both disconnected");
                }
            } else if (disconnectedPlayer == player2) {
                if (!player1.isDisconnected()) {
                    player1.sendMessage("opponent disconnected");
                    player1.sendMessage("GAME_OVER WIN");
                    logger.info("Client on port " + player1.getSocket().getPort() + " notified of opponent disconnection.");
                    saveGameResult(player1.getUsername(), player2.getUsername(), player1.getUsername()+" wins because opponent disconnected");
                } else {
                    logger.info("Both players disconnected. Ending session.");
                    saveGameResult(player1.getUsername(), player2.getUsername(), "nobody wins because both disconnected");
                }
            }
            closeSession();
        } finally {
            lock.unlock();
        }
    }
    private void closeSession() {
        try {
            if (player1 != null && !player1.isDisconnected()) {
                player1.close();
            }
            if (player2 != null && !player2.isDisconnected()) {
                player2.close();
            }
        } catch (IOException e) {
            logger.severe("Error closing client connections: " + e.getMessage());
            e.printStackTrace();
        }
        logger.info("Session resources released at " + new Date());
        System.out.println("resources released");
    }

    private void saveGameResult(String player1, String player2, String result) {
        String historyFilePath = FileManager.getHistoryFilePath();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        System.out.println("writing into history");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(historyFilePath, true))) {
            bw.write(player1 + "," + player2 + "," + result + "," + timestamp);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}