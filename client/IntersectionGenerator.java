package de.ibr.v2x.data.client;

import com.fasterxml.jackson.databind.JsonNode;
import de.ibr.v2x.data.models.Connection;
import de.ibr.v2x.data.models.Coordinate;
import de.ibr.v2x.data.models.Intersection;
import de.ibr.v2x.data.models.Lane;

import java.util.ArrayList;
import java.util.HashMap;

public class IntersectionGenerator {

	public IntersectionGenerator(JsonNode json) throws NullPointerException {
		checkForIntersection(json);
	}

	private void checkForIntersection(JsonNode json) throws NullPointerException {
		//Get Krezungsdata - _source | layers | its | dsrc.MapData_element (dsrc.SPAT_element???) | dsrc.intersections_tree
			json = json.get("_source").get("layers").get("its").get("dsrc.MapData_element").get("dsrc.intersections_tree").get("Item 0");

			JsonNode geo = json.get("dsrc.IntersectionGeometry_element");
			int id = geo.get("dsrc.id_element").get("dsrc.id").asInt();
			String name = geo.get("dsrc.name").asText();
			double lat = geo.get("dsrc.refPoint_element").get("dsrc.lat").asDouble() / 10000000;
			double lon = geo.get("dsrc.refPoint_element").get("dsrc.long").asDouble() / 10000000;
			Intersection intersection = new Intersection(id, name, lat, lon);
			if(!V2XClient.getInstance().getIntersections().contains(intersection)) {
				intersection.setLanes(processLanes(geo, intersection));
				System.out.println(intersection);
				V2XClient.getInstance().addIntersection(intersection);
				new OSMGenerator(intersection);
			}

            /*
            ID: 227 Name: LSA227 Lat:52.2760495 Long: 10.5387248 Edeka Hanssommerstrasse
            ID: 82 Name: K082 Tunicastrasse Lat:52.2745323 Long: 10.5149574 HEM Tunicastrasse
            ID: 6 Name: LSA6 Lat:52.2753772 Long: 10.5218489 Hamburger/Rebenring
            ID: 36 Name: LSA36 Lat:52.2754574 Long: 10.52441 TU Rebenring
            ID: 15 Name: LSA15 Lat:52.275493 Long: 10.5259685 Mensa TU
            ID: 61 Name: LSA61 Lat:52.2756568 Long: 10.5286603 Haus der Wissenschaft
            */

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
