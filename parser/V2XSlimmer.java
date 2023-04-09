package de.ibr.v2x.data.parser;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import de.ibr.v2x.data.models.Connection;
import de.ibr.v2x.data.models.Coordinate;
import de.ibr.v2x.data.models.Intersection;
import de.ibr.v2x.data.models.Lane;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.PCapPacket;
import io.pkts.packet.Packet;
import io.pkts.protocol.Protocol;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class V2XSlimmer {

	public V2XSlimmer() throws IOException {
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current absolute path is: " + s);
		getFiles(new File(s + "/pcap/"));
		//getFiles(new File("/Users/maxgao/Downloads/"));
		//pcap();

	}



	public void getFiles(File folder) throws IOException {
		for (File f : folder.listFiles()) {
			if (f.getName().contains("pcap")) {
				System.out.println(f.getName());
				//String s = f.getName().split("pcap")[0].split("capture-")[1] + "json";

				String s = f.getName().split("pcap")[0].split("capture-")[1] + "json";
				File n = new File(s);
				try {
					parse(f, n);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public void parse(File f, File n) throws IOException {
		HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();
		ObjectMapper mapper = new ObjectMapper();

		char[] buffer = new char[1];
		String prefix = "/bin/bash";
		String c = "-c";
		System.out.println(f.getAbsolutePath());
		String tshark = "tshark -r " + f.getAbsolutePath() + " -T json";

		ProcessBuilder pb = new ProcessBuilder(new String[] {prefix, c, tshark});
		Process p = pb.start();
		JsonFactory jsonFactory = new JsonFactory();

		BufferedWriter writer = new BufferedWriter(new FileWriter(n));
		writer.write("[" + System.getProperty("line.separator"));

		// Create a JsonParser instance
		try (JsonParser jsonParser = jsonFactory.createParser(p.getInputStream())) {

			// Check the first token
			if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
				throw new IllegalStateException("Expected content to be an array");
			}

			// Iterate over the tokens until the end of the array
			while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
				JsonNode node = mapper.readTree(jsonParser);
				if(processV2X(node, intersections)) {
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
					LocalDateTime now = LocalDateTime.now();
					System.out.println(dtf.format(now));
					writer.write(node.toPrettyString() + "," + System.getProperty("line.separator"));
				}
			}
		}

		writer.write("]");
		writer.close();

	}

	public void parseFile(File f, File n) throws IOException {
		HashMap<Integer, Intersection> intersections = new HashMap<Integer, Intersection>();
		ObjectMapper mapper = new ObjectMapper();

		char[] buffer = new char[1];
		String prefix = "/bin/bash";
		String c = "-c";
		System.out.println(f.getAbsolutePath());
		String tshark = "tshark -r " + f.getAbsolutePath() + " -T json";

		ProcessBuilder pb = new ProcessBuilder(new String[] {prefix, c, tshark});
		Process p = pb.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		int counter = 0;
		String s = "";
		BufferedWriter writer = new BufferedWriter(new FileWriter(n));
		writer.write("[" + System.getProperty("line.separator"));

		while (in.read(buffer) != -1) {
			switch (buffer[0]) {
				case '[': {
					continue;
				}
				case '{': {
					counter++;
					break;
				}
				case '}': {
					counter--;
					break;
				}
				default: {
					break;
				}
			}
			s = s + buffer[0];

			if(counter == 0) {
				if(s.length() == 1) { //if current char is only ,
					s = "";
					continue;
				}
				if(processV2X(mapper.readTree(s), intersections)) {
					writer.write(s + "," + System.getProperty("line.separator"));
				}

				s = "";
			}
		}

		writer.write("]");
		in.close();
		writer.close();
	}

	private boolean processV2X(JsonNode json, HashMap intersections) {

		if(json.toString().contains("CAM") || json.toString().contains("cam") || json.toString().contains("Cam")) {
			return true;
		}
		//Output CAM Only in console

		/*
        if(json.toString().contains("SPAT") || json.toString().contains("spat") || json.toString().contains("Spat")) {
            return true;
        }
        //Output SPAT Only in console
		*/
		if(checkForTrafficLight(json, intersections)) {
			return true;
		}
		if(checkForIntersection(json, intersections)) {
			return true;
		}


		return false;
	}

	private boolean checkForTrafficLight(JsonNode json, HashMap intersections) {
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
								return true;
								//PUBLISH UPDATE
							}
						}
					}
				}
			}



		} catch(NullPointerException e) {

		}
		return false;
	}

	private boolean checkForIntersection(JsonNode json, HashMap intersections) {
		//Get Krezungsdata - _source | layers | its | dsrc.MapData_element (dsrc.SPAT_element???) | dsrc.intersections_tree
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
				return true;
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

		}
		return false;
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
}
