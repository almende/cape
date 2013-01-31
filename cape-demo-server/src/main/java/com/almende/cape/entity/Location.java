package com.almende.cape.entity;

import java.io.Serializable;

import com.fasterxml.jackson.databind.node.ObjectNode;

@SuppressWarnings("serial")
public class Location implements Serializable {
	public Location () {}
	
	public Location(Double lat, Double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public Location(ObjectNode location) {
		if (location != null) {
			if (location.has("lat") && !location.get("lat").isNull()) {
				lat = location.get("lat").asDouble();
			}
			if (location.has("lng") && !location.get("lng").isNull()) {
				lng = location.get("lng").asDouble();
			}
		}
	}
	
	// location
	public Double lat = null;
	public Double lng = null;
}
