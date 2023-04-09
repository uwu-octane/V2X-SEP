package de.ibr.v2x.data.replay;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ibr.v2x.data.client.OSMGenerator;
import de.ibr.v2x.data.client.V2XClient;
import de.ibr.v2x.data.models.*;
import de.ibr.v2x.data.server.V2XServer;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;

public class V2XReplay {

	private BlockingQueue queue;
	private int intersectionID;
	private Date start;
	private HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();

	private File f;

	public boolean stop = false;

	private int multiplier = 1;

	SimpleDateFormat dateFormat =new SimpleDateFormat("HH:mm:ss yyyyMMdd");
	public V2XReplay() {}
	public V2XReplay(String s, int multiplier, BlockingQueue queue) throws IOException, InterruptedException {
		this.multiplier = multiplier;
		this.queue = queue;
		f = getFile(s);
		if(f == null) {
			queue.put("Error: Can't find data for this date");
			return;
		}
		File map = new File(f.getAbsolutePath().split(".json")[0] + "_MAP.json");
		loadDate(map);
	}

	/*
		Protocol:
		loadDate: 20210101
		replay: ID 00:00 -> server sends data the same way as with live data

	 */


	public List<String> getDates() throws IOException, InterruptedException {
		File folder = new File("done");
		File[] listOfFiles = folder.listFiles();
		List<String> list = new ArrayList<String>();
		if(listOfFiles == null) {
			return list;
		}

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().contains(".json") && !listOfFiles[i].getName().startsWith("._")) {
					if(!listOfFiles[i].getName().contains("MAP")) {
						File map = new File(listOfFiles[i].getAbsolutePath().split(".json")[0] + "_MAP.json");
						if(!map.exists()) {
							try {
								writeDates(listOfFiles[i], map);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
						String date = listOfFiles[i].getName().split(".json")[0];
						//ADD MAP DATA PROCESSING
						list.add(date + ":" + loadDate(map));
					}
				}
			}
		}
		return list;
	}

	public static void writeDates(File d, File n) throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(n));
		writer.write("[" + System.getProperty("line.separator"));

		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jsonFactory = new JsonFactory();
		InputStream is = null;
		try {
			is = new FileInputStream(d);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		// Create a JsonParser instance
		try (JsonParser jsonParser = jsonFactory.createParser(is)) {

			// Check the first token
			if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
				throw new IllegalStateException("Expected content to be an array");
			}

			// Iterate over the tokens until the end of the array
			try {
				while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
					try {
						JsonNode node = mapper.readTree(jsonParser);
						if(node.toString().contains("dsrc.MapData_element")) {
							writer.write(node.toPrettyString() + "," + System.getProperty("line.separator"));
						}

					} catch (Exception e){
						//e.printStackTrace();
						continue;
					}
				}
			} catch(Exception e) {
				//e.printStackTrace();
			}

		}

		writer.write("]");
		writer.close();
	}

	private static File getFile(String s) {
		File folder = new File("done");
		File[] listOfFiles = folder.listFiles();
		List<String> list = new ArrayList<String>();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				if (listOfFiles[i].getName().contains(".json")) {
					if (s.equalsIgnoreCase(listOfFiles[i].getName().split(".json")[0])) {
						return listOfFiles[i];
					}
				}
			}
		}
		return null;
	}

	public String loadDate(File map) throws IOException, InterruptedException {
		intersections = new HashMap<Integer, Intersection>();
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory jsonFactory = new JsonFactory();
		InputStream is = null;
		String s = "";
		try {
			is = new FileInputStream(map);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		try (JsonParser jsonParser = jsonFactory.createParser(is)) {

			// Check the first token
			if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
				throw new IllegalStateException("Expected content to be an array");
			}

			// Iterate over the tokens until the end of the array
			while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
				JsonNode node = mapper.readTree(jsonParser);
				s = s + checkForIntersection(node, intersections) + ";";
			}
			return s;
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (InterruptedException e) {
			//e.printStackTrace();
		}
		return s;
	}

	public void select(int intersectionID, String s) throws IOException, InterruptedException, ParseException {
		ObjectMapper mapper = new ObjectMapper();

		dateFormat.setTimeZone(TimeZone.getDefault());
		s = s + " " + f.getName().split(".json")[0];
		System.out.println("Waiting for: " + s);
		this.start = dateFormat.parse(s);
		this.intersectionID = intersectionID;

		Thread t = new Thread(new Runnable() {
			@Override   // annotation to override the run method
			public void run() {
				JsonFactory jsonFactory = new JsonFactory();
				InputStream is = null;
				try {
					is = new FileInputStream(f);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
				try (JsonParser jsonParser = jsonFactory.createParser(is)) {

					// Check the first token
					if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
						throw new IllegalStateException("Expected content to be an array");
					}

					// Iterate over the tokens until the end of the array
					Date last = null;
					while (jsonParser.nextToken() != JsonToken.END_ARRAY && !stop) {
						try {
							JsonNode node = mapper.readTree(jsonParser);
							Long ts = Long.parseLong(node.get("_source").get("layers").get("frame").get("frame.time_epoch").asText().split("\\.")[0]);
							Date d = new Date(ts * 1000);
							if(d.after(start)) {
								processV2X(node, intersections);
								queue.put("Time: " + dateFormat.format(d));
								//Starting here
								if(last == null) { // first sent message
									if(intersections.get(intersectionID) == null) { // intersection not found for this date
										queue.put("Error: No data found for intersection " + intersectionID + " on this date");
									}
									last = d;
								}
								//Process node
								long diff = (d.getTime() - last.getTime()) * (1/multiplier);
								Thread.sleep(diff);

								last = d;

							}
						} catch (Exception e){
							//e.printStackTrace();
							continue;
						}


					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		t.start();

	}

	private void processV2X(JsonNode json, HashMap intersections) throws InterruptedException {

		checkForTrafficLight(json, intersections);
		checkForCar(json);

	}

	private void checkForTrafficLight(JsonNode json, HashMap intersections) throws InterruptedException {
		//Get Krezungsdata - _source | layers | its | dsrc.MapData_element (dsrc.SPAT_element???) | dsrc.intersections_tree
		try {
			json = json.get("_source").get("layers").get("its").get("dsrc.SPAT_element").get("dsrc.intersections_tree").get("Item 0").get("dsrc.IntersectionState_element");

			int intersectionID = json.get("dsrc.id_element").get("dsrc.id").asInt();
			Intersection intersection = (Intersection) intersections.get(intersectionID);
			for(JsonNode node : json.get("dsrc.states_tree")) {
				int signalGroup = node.get("dsrc.MovementState_element").get("dsrc.signalGroup").asInt();
				int eventState = node.get("dsrc.MovementState_element").get("dsrc.state_time_speed_tree").get("Item 0").get("dsrc.MovementEvent_element").get("dsrc.eventState").asInt();
				for(Lane l : intersection.getLanes().values()) {
					for(Connection c : l.getConnections()) {
						if(c.getSignalGroup() == signalGroup) {
							if(c.getEventState() != eventState) {
								c.setEventState(eventState);
								if(intersectionID == this.intersectionID) {
									String msg = String.format("TrafficLight;{\"intersection\":%d, \"signalGroup\":%d, \"eventState\":%d}", intersectionID, signalGroup, eventState);
									V2XServer.addMessage(intersectionID, msg);
								}
							}
						}
					}
				}
			}



		} catch(NullPointerException e) {

		}
	}

	private String checkForIntersection(JsonNode json, HashMap intersections) throws InterruptedException {
		//Get Krezungsdata - _source | layers | its | dsrc.MapData_element (dsrc.SPAT_element???) | dsrc.intersections_tree
		String s = "";
		try {
			json = json.get("_source").get("layers").get("its").get("dsrc.MapData_element").get("dsrc.intersections_tree").get("Item 0");

			JsonNode geo = json.get("dsrc.IntersectionGeometry_element");
			int id = geo.get("dsrc.id_element").get("dsrc.id").asInt();
			String name = geo.get("dsrc.name").asText();
			double lat = geo.get("dsrc.refPoint_element").get("dsrc.lat").asDouble() / 10000000;
			double lon = geo.get("dsrc.refPoint_element").get("dsrc.long").asDouble() / 10000000;
			Intersection intersection = new Intersection(id, name, lat, lon);
			if(!intersections.values().contains(intersection)) {
				intersection.setLanes(processLanes(geo, intersection));
				intersections.put(id, intersection);
				new OSMGenerator(intersection);
				s = intersection.getId() + " " + intersection.getName();
				queue.put(intersection.getId() + " " + intersection.getName()); // SEND Intersections
				return s;
			}

            /*
            ID: 227 Name: LSA227 Lat:52.2760495 Long: 10.5387248 Edeka Hanssommerstrasse
            ID: 82 Name: K082 Tunicastrasse Lat:52.2745323 Long: 10.5149574 HEM Tunicastrasse
            ID: 6 Name: LSA6 Lat:52.2753772 Long: 10.5218489 Hamburger/Rebenring
            ID: 36 Name: LSA36 Lat:52.2754574 Long: 10.52441 TU Rebenring
            ID: 15 Name: LSA15 Lat:52.275493 Long: 10.5259685 Mensa TU
            ID: 61 Name: LSA61 Lat:52.2756568 Long: 10.5286603 Haus der Wissenschaft
            */

		} catch(NullPointerException e) {
			return s;
		}
		return s;
	}

	private HashMap<Integer, Lane> processLanes(JsonNode json, Intersection intersection) {
		try {
			JsonNode lanes = json.get("dsrc.laneSet_tree");
			JsonNode nodes;
			Coordinate lastCoordinate;
			HashMap<Integer, Lane> mapLanes = new HashMap<Integer, Lane>();
			ArrayList<Coordinate> lanePositions;
			for(JsonNode lane : lanes) {
				lane = lane.get("dsrc.GenericLane_element");
				nodes = lane.get("dsrc.nodeList_tree").get("dsrc.nodes_tree");
				lastCoordinate = intersection.getCoordinate();
				int laneID = lane.get("dsrc.laneID").asInt();
				lanePositions = new ArrayList<Coordinate>();
				for(JsonNode node : nodes) {
					JsonNode delta = node.get("dsrc.NodeXY_element").get("dsrc.delta_tree");
					for(JsonNode deltaData : delta) {
						double x = deltaData.get("dsrc.x").asDouble();
						double y = deltaData.get("dsrc.y").asDouble();
						double lat = lastCoordinate.getLat() + (y / 100 / 111111);
						double lon = lastCoordinate.getLon() + (x / 100 / (111111 * Math.cos(Math.toRadians(lastCoordinate.getLat()))));
						lastCoordinate = new Coordinate(lat, lon);
						lanePositions.add(lastCoordinate);
					}
				}

				Coordinate next;
				if(lanePositions.get(1) == null) {
					next = lanePositions.get(0);
				} else {
					next = lanePositions.get(1);
				}
				ArrayList<Connection> connections = new ArrayList<Connection>();
				if(lane.get("dsrc.connectsTo_tree") != null) {
					for(JsonNode connection : lane.get("dsrc.connectsTo_tree")) {
						int connectingLane = connection.get("dsrc.Connection_element").get("dsrc.connectingLane_element").get("dsrc.lane").asInt();
						int signalGroup = connection.get("dsrc.Connection_element").get("dsrc.signalGroup").asInt();
						Connection c = new Connection(connectingLane, signalGroup);
						connections.add(c);
					}
				}
				JsonNode laneAttributes = lane.get("dsrc.laneAttributes_element");

				int laneType = laneAttributes.get("dsrc.laneType").asInt();
				int ingress = laneAttributes.get("dsrc.directionalUse_tree").get("dsrc.LaneDirection.ingressPath").asInt();
				int egress = laneAttributes.get("dsrc.directionalUse_tree").get("dsrc.LaneDirection.egressPath").asInt();
				Lane l = new Lane(laneID, lanePositions.get(0), next, laneType, ingress, egress, connections);
				mapLanes.put(laneID, l);
				//lanePostions = Marker
			}

			for(Lane l : mapLanes.values()) {
				Coordinate start = l.getPosition();

				for(Connection connection : l.getConnections()) {
					Coordinate end = mapLanes.get(connection.getLane()).getPosition();
				}
			}
			return mapLanes;

		} catch(NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void checkForCar(JsonNode json) throws InterruptedException {
		try {
			json = json.get("_source").get("layers").get("its").get("cam.CoopAwareness_element").get("cam.camParameters_element");
			Car car = new Car();
			double lat = json.get("cam.basicContainer_element").get("cam.referencePosition_element").get("its.latitude").asDouble() / 10000000;
			double lon = json.get("cam.basicContainer_element").get("cam.referencePosition_element").get("its.longitude").asDouble() / 10000000;
			//car.setAltitude(json.get("cam.basicContainer_element").get("cam.referencePosition_element").get("its.altitude_element").get("its.altitudeValue").asDouble());
			Coordinate coordinate = new Coordinate(lat, lon);
			car.setCoordinate(coordinate);

			JsonNode basicInfor = json.get("cam.highFrequencyContainer_tree").get("cam.basicVehicleContainerHighFrequency_element");
			car.setSpeed(basicInfor.get("cam.speed_element").get("its.speedValue").asInt());
			car.setLength(basicInfor.get("cam.vehicleLength_element").get("its.vehicleLengthValue").asInt());
			car.setWidth(basicInfor.get("cam.vehicleWidth").asInt());
			car.setLateralAcceleration(basicInfor.get("cam.lateralAcceleration_element").get("its.lateralAccelerationValue").asInt());
			car.setLongitudinalAcceleratio(basicInfor.get("cam.longitudinalAcceleration_element").get("its.longitudinalAccelerationValue").asInt());
			car.setHeading(basicInfor.get("cam.heading_element").get("its.headingValue").asInt());

			if (json.get("cam.lowFrequencyContainer_tree") != null) {
				basicInfor = json.get("cam.lowFrequencyContainer_tree").get("cam.basicVehicleContainerLowFrequency_element");
				if (basicInfor != null) {
					ArrayList<Pathhistory> pathhistoriesList = new ArrayList<>();
					JsonNode pathHistorys = basicInfor.get("cam.pathHistory_tree");
					for (JsonNode pathHistoryElement : pathHistorys) {
						Pathhistory pathhistory = new Pathhistory();
						pathHistoryElement = pathHistoryElement.get("its.PathPoint_element");
						//pathhistory.setDeltaAltitude(pathHistoryElement.get("its.pathPosition_element").get("its.deltaAltitude").asDouble());
						double deltaLatitude = pathHistoryElement.get("its.pathPosition_element").get("its.deltaLatitude").asDouble();
						double deltaLongitude = pathHistoryElement.get("its.pathPosition_element").get("its.deltaLongitude").asDouble();
						pathhistory.setDeltaLatitude(deltaLatitude);
						pathhistory.setDeltaLongitude(deltaLongitude);
						pathhistory.setPathDeltaTime(pathHistoryElement.get("its.pathDeltaTime").asInt());

						Coordinate pathcoordinate = new Coordinate(lat + deltaLatitude, lon + deltaLongitude);
						pathhistory.setCoordinate(pathcoordinate);
						pathhistoriesList.add(pathhistory);
					}
					car.setPathhistories(pathhistoriesList);
				}
			}

			queue.put("Car;" + car.toString());

		} catch(NullPointerException e) {

		}
	}

	public void stop() {
		this.intersectionID = 0;
		this.stop = true;
	}

	public Intersection getIntersection() {
		return intersections.get(intersectionID);
	}
}
