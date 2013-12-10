package com.almende.cape.agent;

import java.util.Calendar;
import java.util.HashMap;

import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StateAgent extends CapeStateAgent {
	/**
	 * Get the current state
	 * @param state    Available values: "location"
	 * @return
	 */
	@Override
	public Object getState(@Name("state") String state) {
		if (state.equals("location")) {
			return getState().get("location", Object.class);
		}
		else {
			// TODO: implement other states
			return null;
		}
	}
	
	/**
	 * Change the current location
	 * @param lat
	 * @param lng
	 * @param description
	 * @throws Exception
	 */
	public void setLocation(
			@Optional @Name("lat") Double lat, 
			@Optional @Name("lng") Double lng, 
			@Optional @Name("description") String description) throws Exception {
		// store the new location
		HashMap<String, Object> location = new HashMap<String, Object>();
		if (lat != null) {
			location.put("lat", lat);
		}
		if (lng != null) {
			location.put("lng", lng);
		}
		if (description != null) {
			location.put("description", description);
		}
		getState().put("location", location);
		
		// trigger a change event
		ObjectNode params = JOM.createObjectNode();
		params.put("location", JOM.getInstance().convertValue(location, ObjectNode.class));
		getEventsFactory().trigger("change", params);
	}
	
	/**
	 * Start simulation of the location
	 */
	public void startSimulation() {
		onSimulate();
	}
	
	/**
	 * Stop simulation of the location
	 */
	public void stopSimulation() {
		String taskId = getState().get("taskId", String.class);
		if (taskId != null) {
			getScheduler().cancelTask(taskId);
			getState().remove("taskId");
		}
	}
	
	/**
	 * Perform a simulation step: update location, and start a timer for the
	 * next simulation step.
	 */
	public void onSimulate() {
		stopSimulation();
		
		updateLocation();
		
		long delay = Math.round((10 + 10 * Math.random()) * 1000); // 10-20 sec
		JSONRequest request = new JSONRequest("onSimulate", null);
		String taskId = getScheduler().createTask(request, delay);
		getState().put("taskId", taskId);
	}
	
	/**
	 * Update the location based on a simple time based algorithm:
	 * The location will circle around Rotterdam once an hour.
	 */
	public void updateLocation() {
		Calendar calendar = Calendar.getInstance();
		double minutes = calendar.get(Calendar.MINUTE);
		double seconds = calendar.get(Calendar.SECOND);

		// circle around Rotterdam once an hour
		double t = (minutes + seconds / 60) / 60 * 2 * Math.PI;
		Double lat = 51.9217 + 0.4 * Math.sin(t); 
		Double lng = 4.4811 + 0.4 * Math.cos(t);
		
		try {
			setLocation(lat, lng, null);
		} catch (Exception e) {
		}
	}
	
	@Override
	public String getDescription() {
		return "Hi there, I'm a demo agent for Cape, providing state information! " +
				"Use startSimulation() and stopSimulation() to simulate a " +
				"changing location, and use getState(\"location\") to retrieve " +
				"the current location.";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
