package de.ibr.v2x.data.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class V2XServer {

	final int port = 9999;
	private static HashMap<V2XServerThread, LinkedBlockingQueue<String>> queues;

	public V2XServer() {
		queues = new HashMap<V2XServerThread, LinkedBlockingQueue<String>>();
		start();
	}

	public static void addMessage(int id, String message) {
		for(V2XServerThread thread : queues.keySet()) {
			if(thread.intersectionID == id || id == -1) {
				if(thread.live) {
					try {
						thread.queue.put(message);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}


	public void start() {
		ServerSocket serverSocket = null;
		Socket socket = null;
		System.out.println("Binding Server Socket");
		try {
			serverSocket = new ServerSocket(9999);
		} catch (IOException e) {
			e.printStackTrace();

		}
		System.out.println("Server listening on port 9999");
		while (true) {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println("I/O error: " + e);
			}
			// new thread for a client
			LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
			V2XServerThread t  = new V2XServerThread(socket, queue);
			t.start();
			queues.put(t, queue);

		}
	}

}
