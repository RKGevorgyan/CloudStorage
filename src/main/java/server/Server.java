package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: 02.03.2021
// notify about connect / disconnect

public class Server {
	private final int PORT = 1235;
	private int count;


	public Server() {
		try (ServerSocket server = new ServerSocket(PORT)){
			System.out.println("Server started");
			while (true) {
				System.out.println("Server is waiting connection");
				Socket socket = server.accept();
				System.out.println("Client connected");
				count++;
				ClientHandler clientHandler = new ClientHandler(socket);
				clientHandler.setUserCount(count);
				ExecutorService service = Executors.newCachedThreadPool();
				service.execute(clientHandler);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server();
	}
}
