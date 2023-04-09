package de.ibr.v2x.data.models;

import java.util.ArrayList;
import java.util.Objects;

public class Car {
    //private double latitude;
    //private double longitude;
    //private double altitude;
    private Coordinate coordinate;
    private int speed;
    //private int driveDirection;//???
    private int length;
    private int width;
    private int longitudinalAcceleratio;
    private int lateralAcceleration;
    private int heading;
    private ArrayList<Pathhistory> pathhistories;

    public ArrayList<Pathhistory> getPathhistories() {
        return pathhistories;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    /*public double getAltitude() {
        return altitude;}*/

    public double getLatitude() {
        return coordinate.getLat();
    }

    public double getLongitude() {
        return coordinate.getLon();
    }

    public int getLateralAcceleration() {
        return lateralAcceleration;
    }

    public int getHeading() {
        return heading;
    }

    public int getLength() {
        return length;
    }

    public int getLongitudinalAcceleratio() {
        return longitudinalAcceleratio;
    }

    public int getSpeed() {
        return speed;
    }

    public int getWidth() {
        return width;
    }

    /*public void setAltitude(double altitude) {
        this.altitude = altitude;
    }*/

    public void setLateralAcceleration(int lateralAcceleration) {
        this.lateralAcceleration = lateralAcceleration;
    }

    /*public void setLatitude(double latitude) {
        this.latitude = latitude;
    }*/

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    /*public void setLongitude(double longitude) {
        this.longitude = longitude;
    }*/

    public void setLongitudinalAcceleratio(int longitudinalAcceleratio) {
        this.longitudinalAcceleratio = longitudinalAcceleratio;
    }

    public void setPathhistories(ArrayList<Pathhistory> pathhistories) {
        this.pathhistories = pathhistories;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return speed == car.speed && length == car.length && width == car.width && longitudinalAcceleratio == car.longitudinalAcceleratio && lateralAcceleration == car.lateralAcceleration && heading == car.heading && Objects.equals(coordinate, car.coordinate) && Objects.equals(pathhistories, car.pathhistories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate, speed, length, width, longitudinalAcceleratio, lateralAcceleration, heading, pathhistories);
    }

    @Override
    public String toString() {
        return String.format("{\"lat\":%s, \"lon\":%s}", coordinate.getLat(), coordinate.getLon());
        /*return "{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                "lat" + coordinate.getLat() +
                ", lon" + coordinate.getLon() +
                ", speed:"  + speed +
                ", length:" + length +
                ", width:" + width +
                ", longitudinalAcceleration:" + longitudinalAcceleratio +
                ", lateralAcceleration:" + lateralAcceleration +
                ", heading:" + heading +
                ", pathhistories:" + pathhistories +
                '}';
                */
    }
}
