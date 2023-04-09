package de.ibr.v2x.data.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Intersection {

	private int id;
	private String name;
	private double lat;
	private double lon;
	private Coordinate coordinate;

	public HashMap<Integer, Lane> getLanes() {
		return lanes;
	}

	public void setLanes(HashMap<Integer, Lane> lanes) {
		this.lanes = lanes;
	}

	private HashMap<Integer, Lane> lanes = new HashMap<Integer, Lane>();

	public Intersection(int id, String name, double lat, double lon) {
		this.id = id;
		this.name = name;
		this.lat = lat;
		this.lon = lon;
		coordinate = new Coordinate(lat, lon);
	}

	@Override
	public String toString() {
		return "Intersection{" +
				"id=" + id +
				", name='" + name + '\'' +
				", lat=" + lat +
				", lon=" + lon +
				'}';
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Intersection that = (Intersection) o;
		return id == that.id && Double.compare(that.lat, lat) == 0 && Double.compare(that.lon, lon) == 0 && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, lat, lon);
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}
}
