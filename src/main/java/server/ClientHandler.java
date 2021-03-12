package server;

import java.io.*;
import java.net.Socket;

/**
 * Обработчик входящих клиентов
 */
public class ClientHandler implements Runnable {
	private final Socket socket;
	private int userCount;

	public ClientHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		// Проверяем не вышел ли клиент

		new Thread(() -> {
			while (true){
				if (this.socket.isClosed()) {
					System.out.println("Client " + userCount + " Disconnected");
					break;
				}
			}
		}).start();

		try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		     DataInputStream in = new DataInputStream(socket.getInputStream())) {
				while (true) {
					boolean flag = false;
					try {
						String command = in.readUTF();
						String path = null;
						if ("upload".equals(command)) {
							path = "server";
							flag = true;
						} else if ("download".equals(command)) {
							path = "client";
							flag = true;
						} else if ("remove".equals(command)) {
							path = "server";
							try {
								File file = new File(path + File.separator + in.readUTF());
								file.delete();
							} catch (IOException e) {
								System.out.println("Error while deleting file: no such file or directory");
							}
						}
						if(flag){
						try {
							File file = new File(path + File.separator + in.readUTF());
							if (!file.exists()) {
								file.createNewFile();
							}
							long size = in.readLong();
							FileOutputStream fos = new FileOutputStream(file);
							byte[] buffer = new byte[256];
							for (int i = 0; i < (size + 255) / 256; i++) { // FIXME
								int read = in.read(buffer);
								fos.write(buffer, 0, read);
							}
							fos.close();
							out.writeUTF("DONE");
						} catch (Exception e) {
							out.writeUTF("ERROR");
						}
					}
					}catch (Exception e){
						System.out.println("Unable to read from closed Thread");
						break;
					}
				}
			} catch(IOException e){
				e.printStackTrace();
			}
		}

	public void setUserCount(int userCount) {
		this.userCount = userCount;
	}
}
