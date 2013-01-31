package com.almende.cape.agent;

import java.util.ArrayList;
import java.util.HashSet;

import com.almende.eve.context.Context;

@SuppressWarnings("unchecked")
public class BuildingAgent extends CapeAgent {
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
	public void registerUser(String url) throws Exception {
		Context context = getContext();
		// Get the list of registered agents by url. Create it if necessary.

		HashSet<String> AgentsSet = (HashSet<String>) context.get("AgentsSet");
		if (AgentsSet == null) {
			AgentsSet = new HashSet<String>();
		}
		// Add the url.
		AgentsSet.add(url);
		// Try to put back the list of agents into the context.
		while (context.put("AgentsSet",AgentsSet) == null) {
			// Failure means that the list of agents was modified in the meantime.
			// We need to check if url was already added by someone else,
			// otherwise we re-fetch the updated list of agents, add url and retry.
			AgentsSet = (HashSet<String>) context.get("AgentsSet");
			if (AgentsSet.contains(url)) {
				continue;
			}
			AgentsSet.add(url);
		}
		// Subscribe the added agent to the change location event.
		@SuppressWarnings("unused")
		String subscriptionId = subscribe(url,"change","onChange");
	}
	public Integer numberOfUsers() {
		Context context = getContext();
		HashSet<String> AgentsSet = (HashSet<String>) context.get("AgentsSet");
		if (AgentsSet == null) {
			return 0;
		}
		return AgentsSet.size();
	}
	public ArrayList<String> getUsers() {
		ArrayList<String> retval = new ArrayList<String>();
		Context context = getContext();
		HashSet<String> AgentsSet = (HashSet<String>) context.get("AgentsSet");
		if (AgentsSet == null) {
			return retval;
		}		
		for (String url: AgentsSet) {
			retval.add(url);
		}
		return retval;
	}
	public void unregisterUser(String url) {
		Context context = getContext();
		HashSet<String> AgentsSet = (HashSet<String>) context.get("AgentsSet");
		if (AgentsSet == null) {
			return;
		}
		AgentsSet.remove(url);
		while (context.put("AgentsSet",AgentsSet) == null) {
			AgentsSet = (HashSet<String>) context.get("AgentsSet");
			if (!AgentsSet.contains(url)) {
				continue;
			}
			AgentsSet.remove(url);
		}
		// unsubscribe(url,"change","onChange");
	}
	public void onChange(String url, String event, Object params) {
			
	}
}