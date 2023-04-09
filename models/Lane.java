package de.ibr.v2x.data.models;

import java.util.ArrayList;

public class Lane {

	private double id;
	private Coordinate position;
	private Coordinate position_next;
	private ArrayList<Connection> connections;

	private int laneType;

	private int ingress;
	private int egress;


	public Lane(double id, Coordinate position, Coordinate position_next, int laneType, int ingress, int egress, ArrayList<Connection> connections) {
		this.id = id;
		this.position = position;
		this.position_next = position_next;
		this.laneType = laneType;
		this.ingress = ingress;
		this.egress = egress;
		this.connections = connections;
	}

	public double getId() {
		return id;
	}

	public int getLaneType() { return laneType; }

	public int getIngress() { return ingress; }
	public int getEgress() { return egress; }

	public Coordinate getPosition() {
		return position;
	}

	public Coordinate getPosition_next() {
		return position_next;
	}

	public ArrayList<Connection> getConnections() {
		return connections;
	}


}
