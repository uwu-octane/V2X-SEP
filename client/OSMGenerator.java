package de.ibr.v2x.data.client;

import de.ibr.v2x.data.models.Intersection;
import de.ibr.v2x.data.models.Lane;
import org.osm2world.console.OSM2World;

import java.io.File;
import java.util.Random;

public class OSMGenerator {

	public OSMGenerator(Intersection intersection) {
		File f = new File("osm/" + intersection.getId() + ".obj");
		if(!f.exists()) {
			createBox(intersection);
		}
	}

	public void createBox(Intersection intersection) {
		/*
		double minLat = 90;
		double minLon = 90;
		double maxLat = -90;
		double maxLon = -90;

		for(Lane l : intersection.getLanes().values()) {
			if(minLat > l.getPosition().getLat()) {
				minLat = l.getPosition().getLat();
			}
			if(minLon > l.getPosition().getLon()) {
				minLon = l.getPosition().getLon();
			}
			if(maxLat < l.getPosition().getLat()) {
				maxLat = l.getPosition().getLat();
			}
			if(maxLon < l.getPosition().getLon()) {
				maxLon = l.getPosition().getLon();
			}
		}

		double iLat = intersection.getLat();
		double iLon = intersection.getLon();
		*/
		double iLat = intersection.getLat();
		double iLon = intersection.getLon();
		double x = 15000;
		double y = 15000;
		double maxLat = iLat + (y / 100 / 111111);
		double maxLon = iLon + (x / 100 / (111111 * Math.cos(Math.toRadians(iLat))));

		double minLat = iLat + ((-y) / 100 / 111111);
		double minLon = iLon + ((-x) / 100 / (111111 * Math.cos(Math.toRadians(iLat))));

		create(intersection, minLat, minLon, maxLat, maxLon);
	}

	public void create(Intersection intersection, double minLat, double minLon, double maxLat, double maxLon) {
		System.out.println("[bbox:"+minLat+","+minLon+","+maxLat+","+maxLon+"];"
				+ "(node;rel(bn)->.x;way;node(w)->.x;rel(bw););out meta;");
		String[] args = {"--input_mode", "OVERPASS" , "--input_bbox", minLat + "," + minLon, maxLat + "," + maxLon, "-o", "osm/" + intersection.getId() + ".obj"};
		OSM2World.main(args);
	}


}
