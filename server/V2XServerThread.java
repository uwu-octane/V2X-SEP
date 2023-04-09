package de.ibr.v2x.data.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ibr.v2x.data.client.V2XClient;
import de.ibr.v2x.data.models.Intersection;
import de.ibr.v2x.data.models.Lane;
import de.ibr.v2x.data.replay.V2XReplay;
import org.joda.time.format.DateTimeFormat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class V2XServerThread extends Thread {
	protected Socket socket;
	ObjectMapper objectMapper = new ObjectMapper();
	BlockingQueue<String> queue;

	InputStream inp = null;
	BufferedReader brinp = null;
	DataOutputStream out = null;

	public int intersectionID = 0;

	public boolean live = true;

	private V2XReplay replay;
	public V2XServerThread(Socket clientSocket, LinkedBlockingQueue queue) {
		this.socket = clientSocket;
		this.queue = queue;
	}

	public void run() {
		try {
			inp = socket.getInputStream();
			brinp = new BufferedReader(new InputStreamReader(inp));
			out = new DataOutputStream(socket.getOutputStream());
			if(V2XClient.getInstance().isRunning()) {
				sendMessage("Connected to V2XServer");
			} else {
				sendMessage("Not connected IBR, could be running on cache");
			}
			sendIntersectionList();
			Thread sender= new Thread(new Runnable() {
				String msg;
				@Override   // annotation to override the run method
				public void run() {
					while(true){
						while ((msg = queue.poll()) != null) {
							try {
								sendMessage(msg);
							} catch (IOException e) {
								if(replay != null) {
									replay.stop();
								}
								System.out.println("Client disconnected.");
							}

						}
					}
				}
			});
			sender.start();

			Thread receive= new Thread(new Runnable() {
				String msg ;
				@Override
				public void run() {
					try {
						msg = brinp.readLine();
						while(msg!=null){
							System.out.println("Client:" + msg);
							if(msg.startsWith("selected: ")) {
								try {
									replay = null;
									live = true;
									intersectionID = Integer.parseInt(msg.split("selected: ")[1]);
									selectedIntersection(null);
								} catch (NumberFormatException e) {
									sendMessage("Wrong format, use selected: {ID}");
								}
							}

							if(msg.startsWith("replay: ")) {
								if(replay != null) {
									replay.stop();
								}
								try {
									String data = msg.split("replay: ")[1];
									String date = data.split(" ")[0];
									int id = Integer.parseInt(data.split(" ")[1]);
									intersectionID = id;
									String time = data.split(" ")[2];
									int multiplier = Integer.parseInt(data.split(" ")[3]);
									replay = new V2XReplay(date, multiplier, queue);
									intersectionID = 0;
									live = false;
									replay.select(id, time);
									selectedIntersection(replay.getIntersection());
									sendMessage("Starting Replay");
								} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
									sendMessage("Error: Wrong format, replay: ID TIME(HH:MM:ss) multi");
								} catch (ParseException | InterruptedException e) {
									e.printStackTrace();
									sendMessage("Error: Can't Replay Date" );
								}
							}

							if(msg.equalsIgnoreCase("getIntersectionList")) {
								sendIntersectionList();
							}

							msg = brinp.readLine();
						}

						System.out.println("Client disconnected");
						if(!live) {
							replay.stop();
						}
						out.close();
						socket.close();
					} catch (IOException e) {
					}
				}
			});
			receive.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void selectedIntersection(Intersection i) throws IOException {
		try {
			if(i == null) {
				if(V2XClient.getInstance().getIntersection(intersectionID) != null) {
					i = V2XClient.getInstance().getIntersection(intersectionID);
				} else {
					sendMessage("Error: No Persistent Data for Intersection " + intersectionID);
					return;
				}
			}
			boolean b = false;
			sendMessage("Start of Center for Intersection " + i.getId());
			String intersectionString = "Center;{\"lon\":" + i.getLon() + ", \"lat\":" + i.getLat() + " }";
			sendMessage(intersectionString);
			sendMessage("End of Center for Intersection " + i.getId());
			File obj = new File("osm/" + i.getId() + ".obj");
			File mtl = new File("osm/" + i.getId() + ".obj.mtl");
			sendFile(obj);
			sendFile(mtl);
			sendMessage("Start of Persistent Data for Intersection " + i.getId());
			for(Lane l : V2XClient.getInstance().getIntersection(i.getId()).getLanes().values()) {
				b = true;
				sendMessage(objectMapper.writeValueAsString(l));
			}
			sendMessage("End of Persistent Data for Intersection " + i.getId());
		}
		catch(IOException ex) {
			sendMessage("Internal server error");
		}
	}

	private void sendFile(File f) throws IOException {
		sendMessage("Start of " + f.getName());
		byte[] byteArray = new byte[(int) f.length()];					//creating byteArray with length same as file length
		out.writeInt(byteArray.length);
		BufferedInputStream bis = new BufferedInputStream (new FileInputStream(f));
		//Writing int 0 as a Flag which denotes the file is present in the Server directory, if file was absent, FileNotFound exception will be thrown and int 1 will be written
		out.writeInt(0);

		BufferedOutputStream bos = new BufferedOutputStream(out);

		int count;
		while((count = bis.read(byteArray)) != -1) {			//reads bytes of byteArray length from the BufferedInputStream into byteArray
			bos.write(byteArray, 0, count);					//writes bytes from byteArray into the BufferedOutputStream (0 is the offset and count is the length)
		}
		bos.flush();
		bis.close();
		sendMessage("End of " + f.getName());
	}

	private void sendMessage(String s) throws IOException {
		out.write((s + "\n").getBytes(StandardCharsets.US_ASCII));
		out.flush();
	}

	private void sendIntersectionList() {
		try {
			sendMessage("Start of Intersections");
			for (Intersection intersection : V2XClient.getInstance().getIntersections()) {
				sendMessage(intersection.getId() + " " + intersection.getName());
			}
			sendMessage("End of Intersections");
			sendMessage("Start of Historic Data");
			V2XReplay temp = new V2XReplay();
			for (String s : temp.getDates()) {
				sendMessage(s);
			}
			sendMessage("End of Historic Data");
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}