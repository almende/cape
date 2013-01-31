package com.almende.cape.agent;

import java.util.ArrayList;
import java.util.HashSet;

@SuppressWarnings("unchecked")
public class BuildingAgent extends CapeAgent {
	HashSet<String> agentSet = null;
	
	@Override
	public String getVersion() {
		return "0.1";
	}
	@Override
	public String getDescription() {
		return "The building agent represents a physical building." +
				"Other agents can check register with the building agent, " +
				"which will keep track of which agents are in the building and" +
				"will notify the last agent remaining in the building.";
	}
	
	private HashSet<String> getAgentSet(){
		HashSet<String> agentSet = (HashSet<String>) getContext().get("agentSet");
		if (agentSet == null) {
			agentSet = new HashSet<String>();
			getContext().put("agentSet",agentSet);
		}
		return agentSet;
	}
	private void setAgentSet(HashSet<String> agentSet){
		getContext().put("agentSet", agentSet);
	}
	public void registerUser(String url) throws Exception {
		
		// Get the list of registered agents by url. Create it if necessary.
		agentSet = getAgentSet();
		// Add the url.
		agentSet.add(url);
		// Try to put back the list of agents into the context.
		setAgentSet(agentSet);
		
		// Subscribe the added agent to the change location event.
		@SuppressWarnings("unused")
		String subscriptionId = subscribe(url,"change","onChange");
	}
	public Integer numberOfUsers() {
		HashSet<String> agentSet = getAgentSet();
		if (agentSet == null) {
			return 0;
		}
		return agentSet.size();
	}
	public ArrayList<String> getUsers() {
		ArrayList<String> retval = new ArrayList<String>();
		HashSet<String> agentSet = getAgentSet();
		if (agentSet == null) {
			return retval;
		}
		retval.addAll(agentSet);
		return retval;
	}
	public void unregisterUser(String url) {
		HashSet<String> agentSet = getAgentSet();
		if (agentSet == null) {
			return;
		}
		agentSet.remove(url);
		setAgentSet(agentSet);
		// unsubscribe(url,"change","onChange");
	}
	public void onChange(String url, String event, Object params) {
			
	}
}