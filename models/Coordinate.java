package de.ibr.v2x.data.models;

public class Coordinate {
	private double lat;
	private double lon;

	public Coordinate(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	@Override
	public String toString() {
		return "" + lat +
				", " + lon;
	}
}
