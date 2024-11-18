package org.example.demo;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Server server;
    private int[][] initialBoard;
    private MoveListener MoveListener;
    private GameSession gameSession;
    private boolean disconnected = false;
    private boolean authenticated = false;
    private String username;
    public String getUsername(){
        return username;
    }
    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.server = server;
    }
    public boolean isDisconnected() {
        return disconnected;
    }
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (!authenticated) {
                    if (message.startsWith("REGISTER")) {
                        handleRegister(message);
                    } else if (message.startsWith("LOGIN")) {
                        handleLogin(message);
                    }
                }
                if ("JOIN".equals(message)) {
                    sendMessage("WAITING FOR PLAYER");
                }
                if (message.startsWith("INIT_BOARD")) {
                    initialBoard = parseBoard(message);
                    server.addClientWithBoard(this);
                }
                if (message.startsWith("MOVE ")) {
                    if (MoveListener != null) {
                        MoveListener.onMoveReceived(message, this);  // 调用回调
                    }
                    sendMessage("RECEIVE MOVE");
                }
                if (message.startsWith("HISTORY ")) {
                    String history = getGameHistory(message.split(" ")[1]);
                    sendMessage(history);
                }
                if (message.equals("STATUS")) {
//                    String user = message.split(" ")[1];
//                    String status = getUserStatus(user);
                    String status = getUserStatus();
                    sendMessage(status);
                }
                if (message.startsWith("USER_OFFLINE ")) {
                    setUserOffline(username);  // 更新用户状态为离线
                    System.out.println("set"+username+"offline");
                }
            }
        } catch (IOException e) {
            disconnected = true;
            e.printStackTrace();
        } finally {
            try {
                System.out.println("Client disconnected from port: " + socket.getPort());
                socket.close();
                server.removeClientFromQueue(this);
                if (gameSession != null) {
                    gameSession.handleClientDisconnect(this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int[][] parseBoard(String message) {
        String[] rows = message.replace("INIT_BOARD ", "").split(";");
        int[][] board = new int[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            board[i] = Arrays.stream(rows[i].trim().split("\\s+"))
                    .filter(str -> !str.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        }
        return board;
    }

    public void sendMessage(String message) {
        if (!disconnected) {
            System.out.println("Sending message to client on port " + socket.getPort() + ": " + message);
            out.println(message);
        }
    }

    public void sendBoard(int[][] board) {
        StringBuilder boardStr = new StringBuilder("BOARD " + " ");
        for (int i = 0; i < board.length; i++) {
            for (int cell : board[i]) {
                boardStr.append(cell).append(" ");
            }
            boardStr.append(";");
        }
        sendMessage(boardStr.toString().trim());
    }

    public int[][] getInitialBoard() {
        return initialBoard;
    }

    public Socket getSocket() {
        return socket;
    }
    public void setMoveListener(MoveListener moveListener) {
        this.MoveListener = moveListener;
    }
    public interface MoveListener {
        void onMoveReceived(String move, ClientHandler player);
    }

    public void close() throws IOException {
        disconnected = true;
        if (!socket.isClosed()) {
            socket.close();
        }
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        setUserOffline(this.username);
    }

    public boolean registerUser(String username, String password) {
        String hashedPassword = hashPassword(password);
        String usersFilePath = FileManager.getUsersFilePath();
        File file = new File(usersFilePath);

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username)) {
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(username + "," + hashedPassword + ",offline");
            bw.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loginUser(String username, String password) {
        String hashedPassword = hashPassword(password);
        String usersFilePath = FileManager.getUsersFilePath();
        File file = new File(usersFilePath);
        List<String> lines = new ArrayList<>();
        boolean flag = false;
        boolean flag2 = true;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username) && parts[1].equals(hashedPassword)) {
                    if (parts[2].equals("online"))flag2 = false;
                    parts[2] = "online";
                    lines.add(String.join(",", parts));
                    flag = true;
                    continue;
                }
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String updatedLine : lines) {
                bw.write(updatedLine);
                bw.newLine();
            }
            return flag&&flag2;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error: SHA-256 Algorithm not found", e);
        }
    }

//    private String getUserStatus(String username) {
//        String usersFilePath = FileManager.getUsersFilePath();
//        File file = new File(usersFilePath);
//
//        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                String[] parts = line.split(",");
//                if (parts[0].equals(username)) {
//                    return "STATUS: "+username + " is " + parts[2];
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return "STATUS: User not found";
//    }
    private String getUserStatus() {
        String usersFilePath = FileManager.getUsersFilePath();
        File file = new File(usersFilePath);
        StringBuilder allStatus = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    allStatus.append("STATUS: " + "User: ").append(parts[0])
                            .append(", Status: ").append(parts[2])
                            .append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (allStatus.isEmpty()) {
            return "STATUS: No user status available";
        }

        return allStatus.toString();
    }

    private String getGameHistory(String username) {
        String historyFilePath = FileManager.getHistoryFilePath();
        File file = new File(historyFilePath);
        StringBuilder history = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username) || parts[1].equals(username)) {
                    history.append("HISTORY: "+line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (history.toString().isEmpty())return "HISTORY: no history";
        return history.toString();
    }

    private void handleRegister(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 3) {
            String username = parts[1];
            String password = parts[2];
            if (registerUser(username, password)) {
                sendMessage("REGISTER_SUCCESS");
                System.out.println("REGISTER_SUCCESS");
            } else {
                sendMessage("REGISTER_FAILED");
                System.out.println("REGISTER_FAILED");
            }
        }
    }

    private void handleLogin(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 3) {
            String username = parts[1];
            String password = parts[2];
            if (loginUser(username, password)) {
                this.username = username;
                this.authenticated = true;
                sendMessage("LOGIN_SUCCESS");
                System.out.println("LOGIN_SUCCESS");
                //server.addClientWithBoard(this); // 添加到在线用户列表中
            } else {
                sendMessage("LOGIN_FAILED");
                System.out.println("LOGIN_FAILED");
            }
        }
    }
    private void setUserOffline(String username) {
        String usersFilePath = FileManager.getUsersFilePath();
        File file = new File(usersFilePath);
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts[0].equals(username)) {
                    parts[2] = "offline";
                    lines.add(String.join(",", parts));
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String updatedLine : lines) {
                bw.write(updatedLine);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
