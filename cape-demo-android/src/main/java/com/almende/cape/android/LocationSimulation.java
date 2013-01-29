package com.almende.cape.android;

import com.almende.cape.agent.LocationAgent;
import com.almende.eve.agent.AgentFactory;

public class LocationSimulation {
	private static String AGENT_ID = "location";
	
	private AgentFactory factory = null;
	private LocationAgent agent = null;
	
	public void start(String username, String password) throws Exception {
		if (factory == null) {
			factory = AgentFactory.getInstance();
			if (factory == null) {
				throw new Exception("CAPE is not yet instantiated");
			}
		}
		
		if (agent != null) {
			throw new Exception ("LocationAgent already created");
		}
		agent = (LocationAgent) factory.createAgent(LocationAgent.class, AGENT_ID);
		agent.setAccount(username, password, "location");
		agent.connect();
		agent.startSimulation();
	}
	
	public void stop () throws Exception {
		if (agent != null) {
			agent.stopSimulation();
			agent.disconnect();
			agent = null;
			factory.deleteAgent(AGENT_ID);
		}
	}
}
