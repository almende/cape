package com.almende.cape.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Person implements Serializable {
	public Person() {}
	
	public String agentUrl;
	public String subscriptionId;
	public Boolean present;
	
	public Location location;
}
