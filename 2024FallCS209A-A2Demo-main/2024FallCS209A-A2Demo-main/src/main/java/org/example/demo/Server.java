package org.example.demo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class Server {
    private static final int PORT = 1234;
    protected static final Queue<ClientHandler> waitingQueue = new LinkedList<>();

    public static void main(String[] args) {
        new Server().start();
    }

    public void start() {
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
        System.out.println("Client added to queue from port: " + client.getSocket().getPort());
        if (waitingQueue.size() >= 2) {
            ClientHandler player1 = waitingQueue.poll();
            ClientHandler player2 = waitingQueue.poll();
            assert player2 != null;
            player2.sendMessage("MATCH");
            player1.sendMessage("MATCH");
            int[][] initialBoard = player1.getInitialBoard();
            System.out.println("Match created between clients from ports: " + player1.getSocket().getPort() + " and " + player2.getSocket().getPort());
            GameSession gs = new GameSession(player1, player2, initialBoard);//.start();
            player1.setGameSession(gs);player2.setGameSession(gs);
            gs.start();
        }
    }
    public synchronized void removeClientFromQueue(ClientHandler client) {
        if (waitingQueue.remove(client)) {
            System.out.println("Client removed from queue from port: " + client.getSocket().getPort());
        }
    }
}
