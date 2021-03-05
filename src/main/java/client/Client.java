package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class Client {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;
	private final int PORT = 1235;
	private ArrayList<String> listOfFiles = new ArrayList<>();
	private DefaultListModel<String> dlm = new DefaultListModel<String>();
	String[] str;

	public Client() throws IOException {
		socket = new Socket("localhost", PORT);
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		runClient();

	}

	private void runClient() {
		JFrame frame = new JFrame("Cloud Storage");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);

		JTextArea ta = new JTextArea();
		// TODO: 02.03.2021
		// list file - JList
		File file = new File("server" + File.separator);
		String[] files = file.list();
		for (String s : files) {
			dlm.add(0,s);
		}

		JList<String> jList = new JList<>(dlm);
		JButton uploadButton = new JButton("Upload");
		JButton downloadButton = new JButton("Download");
		JButton removeButton = new JButton("Remove");

		frame.getContentPane().add(BorderLayout.NORTH, ta);
		frame.getContentPane().add(BorderLayout.SOUTH, uploadButton);
		frame.getContentPane().add(BorderLayout.WEST,downloadButton);
		frame.getContentPane().add(BorderLayout.EAST,removeButton);
		frame.getContentPane().add(BorderLayout.CENTER,new JScrollPane(jList));

		frame.setVisible(true);


		uploadButton.addActionListener(a -> {
			System.out.println(sendFile(ta.getText(),"upload"));
			dlm.add(dlm.getSize(),ta.getText());
		});

		downloadButton.addActionListener( a -> {
			System.out.println(sendFile(ta.getText(), "download"));
		});

		removeButton.addActionListener(a -> {
			System.out.println(removeFile(ta.getText(),"remove"));
			for(int i = 0; i < dlm.size(); i++){
				if (dlm.get(i).equals(ta.getText()))
					dlm.remove(i);
			}
		});
	}

	private String sendFile(String filename,String command) {
		String from = null;
		try {
			if (command.equals("upload"))
				from = "client";
			else if (command.equals("download")) {
				from = "server";
			}
			File file = new File(from + File.separator + filename);
			if (file.exists()) {
				out.writeUTF(command);
				out.writeUTF(filename);
				long length = file.length();
				out.writeLong(length);
				FileInputStream fis = new FileInputStream(file);
				int read = 0;
				byte[] buffer = new byte[256];
				while ((read = fis.read(buffer)) != -1) {
					out.write(buffer, 0, read);
				}
				out.flush();
				String status = in.readUTF();
				return status;
			} else {
				return "File is not exists";
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "Something error";
	}

	private String removeFile(String filename, String command){
		try{
			out.writeUTF(command);
			out.writeUTF(filename);
			out.flush();
		}catch (IOException e){
			return "Exception while deleting file";
		}
		return "DONE";
	}

	public static void main(String[] args) throws IOException {
		new Client();
	}
}
