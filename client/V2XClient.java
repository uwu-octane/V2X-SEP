package de.ibr.v2x.data.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ibr.v2x.data.models.Car;
import de.ibr.v2x.data.models.Intersection;
import org.apache.commons.lang.ObjectUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class V2XClient {

	private static V2XClient instance = null;

	private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();

	private boolean isRunning = false;

	public V2XClient() {
		start();
	}

	public static V2XClient getInstance() {
		if(instance == null) {
			instance = new V2XClient();
		}
		return instance;
	}

	public void start() {
		System.out.println("Connecting to V2XAntenna Server");
		Thread TCPListenerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				ObjectMapper mapper = new ObjectMapper();
				JsonFactory jsonFactory = new JsonFactory();
				try {
					Socket clientSocket = new Socket("v2xsep.ibr.cs.tu-bs.de", 9999);
					isRunning = true;
					System.out.println("Connected to V2XAntenna Server");
					try (JsonParser jsonParser = jsonFactory.createParser(clientSocket.getInputStream())) {

						// Check the first token
						if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
							throw new IllegalStateException("Expected content to be an array");
						}

						// Iterate over the tokens until the end of the array
						while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
							try {
								JsonNode node = mapper.readTree(jsonParser);
								processV2X(node);
							} catch (Exception e){
								continue;
							}


						}
					}

				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Can't connect to server, waiting 10s to reconnect");
					isRunning = false;
					try {
						Thread.sleep(10000);
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
					instance = new V2XClient(); //restart of singleton
				}

			}
		});
		TCPListenerThread.start();
	}

	private void processV2X(JsonNode json) {
		try {
			new TrafficLightGenerator(json);
		} catch (NullPointerException e1) {
			try {
				new IntersectionGenerator(json);
			} catch (NullPointerException e2) {
				try {
					new CarGenerator(json);
				} catch (NullPointerException e3) {
					//Unknown Package spotted
					//e3.printStackTrace();
					//System.out.println(json);
				}
			}
		}

	}




	public Intersection getIntersection(int id) {
		return intersections.get(id);
	}
	public void addIntersection(Intersection intersection) {
		intersections.put(intersection.getId(), intersection);
	}

	public Collection<Intersection> getIntersections() {
		return intersections.values();
	}

	public boolean isRunning() {
		return isRunning;
	}

}
