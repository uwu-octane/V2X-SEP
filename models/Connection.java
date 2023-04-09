package de.ibr.v2x.data.models;

public class Connection {
	private int lane;
	private int signalGroup;

	private int eventState = 0;

	public Connection(int lane, int signalGroup) {
		this.lane = lane;
		this.signalGroup = signalGroup;
	}

	public int getEventState() {
		return eventState;
	}

	public void setEventState(int eventState) {
		this.eventState = eventState;
	}

	public int getLane() {
		return lane;
	}

	public int getSignalGroup() {
		return signalGroup;
	}
}
