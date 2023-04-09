package de.ibr.v2x.data.models;

import java.util.Objects;

public class Pathhistory {
    // they are delta coordinate, it should be added with the old coordinate.
    private double  deltaLatitude;
    private double deltaLongitude;
    //private double deltaAltitude;
    private double pathDeltaTime;
    private Coordinate coordinate;

    /*public double getDeltaAltitude() {
        return deltaAltitude;
    }*/

   public double getDeltaLatitude() {
        return deltaLatitude;
    }

    public double getDeltaLongitude() {
        return deltaLongitude;
    }

    public double getPathDeltaTime() {
        return pathDeltaTime;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    /*public void setDeltaAltitude(double deltaAltitude) {
        this.deltaAltitude = deltaAltitude;
    }*/

    public void setDeltaLatitude(double deltaLatitude) {
        this.deltaLatitude = deltaLatitude;
    }

    public void setDeltaLongitude(double deltaLongitude) {
        this.deltaLongitude = deltaLongitude;
    }

    public void setPathDeltaTime(double pathDeltaTime) {
        this.pathDeltaTime = pathDeltaTime;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    @Override
    public String toString() {
        return "Pathhistory{" +
                "deltaLatitude=" + deltaLatitude +
                ", deltaLongitude=" + deltaLongitude +
                ", pathDeltaTime=" + pathDeltaTime +
                ", coordinate=" + coordinate +
                '}';
    }
}
