package org.example.demo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Server {
    private static final int PORT = 1234;
    protected static final Queue<ClientHandler> waitingQueue = new LinkedList<>();
    private final Map<String, GameSession> activeSessions = new HashMap<>();

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server shutting down, closing all sessions...");
            for (GameSession session : activeSessions.values()) {
                session.closeSession(); // 确保所有会话正确关闭
            }
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from port: " + clientSocket.getPort());
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addClientWithBoard(ClientHandler client) {
        waitingQueue.add(client);
        System.out.print("[");
        for (ClientHandler clientHandler : waitingQueue) {
            System.out.print(clientHandler.getUsername()+" ");
        }
        System.out.print("]");
        System.out.println("Client added to queue from port: " + client.getSocket().getPort());
//        if (waitingQueue.size() >= 2) {
//            ClientHandler player1 = waitingQueue.poll();
//            ClientHandler player2 = waitingQueue.poll();
//            assert player2 != null;
//            player2.sendMessage("MATCH");
//            player1.sendMessage("MATCH");
//            int[][] initialBoard = player1.getInitialBoard();
//            System.out.println("Match created between clients from ports: " + player1.getSocket().getPort() + " and " + player2.getSocket().getPort());
//            GameSession gs = new GameSession(player1, player2, initialBoard);//.start();
//            player1.setGameSession(gs);player2.setGameSession(gs);
//            gs.start();
//        }
    }
    public synchronized void removeClientFromQueue(ClientHandler client) {
        if (waitingQueue.remove(client)) {
            System.out.println("Client removed from queue from port: " + client.getSocket().getPort());
        }
    }
    public synchronized void sendQueueToClient(ClientHandler requester) {
        StringBuilder queueList = new StringBuilder("QUEUE ");
        for (ClientHandler clientHandler : waitingQueue) {
            queueList.append(clientHandler.getUsername()).append(" ");
        }
        requester.sendMessage(queueList.toString().trim());
    }
    public synchronized void handleChallenge(ClientHandler challenger, String opponentUsername) {
        ClientHandler opponent = null;
        for (ClientHandler clientHandler : waitingQueue) {
            if (clientHandler.getUsername().equals(opponentUsername)) {
                opponent = clientHandler;
                break;
            }
        }

        if (opponent != null) {
            waitingQueue.remove(challenger);
            waitingQueue.remove(opponent);
            challenger.sendMessage("MATCH");
            opponent.sendMessage("MATCH");
            int[][] initialBoard = challenger.getInitialBoard();
            GameSession gs = new GameSession(challenger, opponent, initialBoard);
            challenger.setGameSession(gs);
            opponent.setGameSession(gs);
            gs.start();
            System.out.println("Match created between clients: " + challenger.getUsername() + " and " + opponentUsername);
        } else {
            challenger.sendMessage("CHALLENGE_FAILED Opponent not found or unavailable.");
            System.out.println("Challenge failed: Opponent " + opponentUsername + " not found or unavailable.");
        }
    }

    public synchronized void registerGameSession(String username, GameSession session) {
        activeSessions.put(username, session);
    }

    public synchronized GameSession getActiveGameSession(String username) {
        return activeSessions.get(username);
    }

    public synchronized boolean reconnectClient(String username, ClientHandler handler) {
        GameSession session = getActiveGameSession(username);
        if (session != null) {
            session.onPlayerReconnect(handler);
            return true;
        }
        return false;
    }

    public synchronized void removeGameSession(String username) {
        if (activeSessions.remove(username) != null) {
            System.out.println("Game session for user " + username + " removed from activeSessions.");
        } else {
            System.out.println("No active game session found for user " + username + ".");
        }
    }

}
//bug是游戏结束退出也要等待重连
//server异常结束应该设置玩家offline