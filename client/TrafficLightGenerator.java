package de.ibr.v2x.data.client;

import com.fasterxml.jackson.databind.JsonNode;
import de.ibr.v2x.data.Main;
import de.ibr.v2x.data.models.Connection;
import de.ibr.v2x.data.models.Coordinate;
import de.ibr.v2x.data.models.Intersection;
import de.ibr.v2x.data.models.Lane;
import de.ibr.v2x.data.server.V2XServer;

import java.util.ArrayList;
import java.util.HashMap;

public class TrafficLightGenerator {

	public TrafficLightGenerator(JsonNode json) throws NullPointerException {
		checkForTrafficLight(json);
	}

	private void checkForTrafficLight(JsonNode json) throws NullPointerException {
		//Get Krezungsdata - _source | layers | its | dsrc.MapData_element (dsrc.SPAT_element???) | dsrc.intersections_tree
			json = json.get("_source").get("layers").get("its").get("dsrc.SPAT_element").get("dsrc.intersections_tree").get("Item 0").get("dsrc.IntersectionState_element");

			int intersectionID = json.get("dsrc.id_element").get("dsrc.id").asInt();
			Intersection intersection = V2XClient.getInstance().getIntersection(intersectionID); //can throw null if no intersections loaded yet
			if(intersection == null) {
				return;
			}
			for(JsonNode node : json.get("dsrc.states_tree")) {
				int signalGroup = node.get("dsrc.MovementState_element").get("dsrc.signalGroup").asInt();
				int eventState = node.get("dsrc.MovementState_element").get("dsrc.state_time_speed_tree").get("Item 0").get("dsrc.MovementEvent_element").get("dsrc.eventState").asInt();
				for(Lane l : intersection.getLanes().values()) {
					for(Connection c : l.getConnections()) {
						if(c.getSignalGroup() == signalGroup) {
							if(c.getEventState() != eventState) {
								c.setEventState(eventState);
								//System.out.println("Intersection: " + intersectionID + " signalGroup " + signalGroup + " is now " + eventState);
								String msg = String.format("TrafficLight;{\"intersection\":%d, \"signalGroup\":%d, \"eventState\":%d}", intersectionID, signalGroup, eventState);
								V2XServer.addMessage(intersectionID, msg);
								//PUBLISH UPDATE
							}
						}
					}
				}
			}

	}

}
