package org.academiadecodigo.loopeytunes.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private Socket playerSocket;
    private LinkedList<ClientConnection> clientConnections;
    private ExecutorService threadPool;
    private BufferedReader in;
    private Game game;
    private int counter = 0;

    private Server() {

        try {

            serverSocket = new ServerSocket(9001);
            clientConnections = new LinkedList<>();
            threadPool = Executors.newFixedThreadPool(4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        awaitingConnections();
    }

    public static void main(String[] args) {
        Server server = new Server();
    }

    private void awaitingConnections() {

        for (int i = 0; i < 4; i++) {

            try {

                playerSocket = serverSocket.accept();

                in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));

                ClientConnection newPlayer = new ClientConnection(playerSocket, in.readLine());
                clientConnections.add(newPlayer);
                threadPool.submit(newPlayer);
                System.out.println(newPlayer.name + " is connected");

            } catch (IOException e) {

                e.printStackTrace();
            }
        }
        sendStory();
        game = new Game();
        sendAll("Game is on!\n");

        next();
    }

    private void sendStory() {
        try {
            File intro = new File("Server/resources/introduction.txt");

            BufferedReader introReader = new BufferedReader(new FileReader(intro));
            String line = "";

            while ((line = introReader.readLine()) != null) {

                if (line.equals("/animation")) {
                    while (!(line = introReader.readLine()).equals("/text")) {
                        sendAll(line);
                    }
                    Thread.sleep(2000);
                    continue;
                }
                sendAll(line);
                Thread.sleep(5000);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void sendAll(String message) {
        for (ClientConnection cc : clientConnections) {
            cc.send(message);
        }
    }

    private void next() {
        if (counter == clientConnections.size()) {
            sendAll(game.getHint());
            counter = 0;
        }
        clientConnections.get(counter).send("It's your turn to guess!");
        counter++;
    }

    private void win() {
        sendAll(game.getConfession());
        sendAll("GAME IS OVER\n");

        for (int i = 0; i < clientConnections.size(); i++) {
            clientConnections.remove(clientConnections.get(i));
        }

        System.out.println("NEW GAME");
        awaitingConnections();
    }

    private class ClientConnection implements Runnable {

        private BufferedReader in;
        private PrintWriter out;
        private Socket playerSocket;
        private final String name;

        private ClientConnection(Socket playerSocket, String name) {

            this.playerSocket = playerSocket;
            this.name = name;

            try {

                in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(playerSocket.getOutputStream()));

            } catch (IOException e) {
                System.out.println("Unable to establish a connection to client.");
                System.exit(0);
            }
        }

        public void close() {
            try {
                playerSocket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            String guess;
            try {
                while ((guess = in.readLine()) != null) {

                    sendAll(guess);

                    if (game.check(guess)) {
                        sendAll("Detective " + name + " WINS THE GAME");
                        win();
                    } else {
                        sendAll("Detective " + name + " failed! Next!\n");
                    }

                    next();
                }

                close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void send(String message) {
            out.println(message);
            out.flush();
        }
    }
}