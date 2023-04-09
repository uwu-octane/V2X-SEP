package de.ibr.v2x.data.client;

import com.fasterxml.jackson.databind.JsonNode;
import de.ibr.v2x.data.models.*;
import de.ibr.v2x.data.server.V2XServer;

import java.util.ArrayList;

public class CarGenerator {

	public CarGenerator(JsonNode json) throws NullPointerException {
		checkForCar(json);
	}

	private void checkForCar(JsonNode json) throws NullPointerException  {
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

			V2XServer.addMessage(-1, "Car;" + car.toString());

	}
}
