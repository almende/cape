package com.almende.cape.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Person implements Serializable {
	public Person() {}
	
	public String userId;
	public String agentUrl;
	public String subscriptionId;

	public Location location;
	public Double distance; // distance in kilometers
	public Boolean present; // true if within a range of 100 meters
}
