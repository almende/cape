package com.almende.cape.agent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocationAgent extends CapeStateAgent {
	/**
	 * Get the current state
	 * @param state    Available values: "location"
	 * @return
	 * @throws Exception 
	 */
	@Override
	public Object getState(@Name("state") String state) throws Exception {
		if (state.equals("location")) {
			return getContext().get("location");
		}
		else {
			// no information available for other states
			throw new Exception("No information available for state '" + state + "', " +
					"only information for 'location' is available.");
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
			@Required(false) @Name("lat") Double lat, 
			@Required(false) @Name("lng") Double lng, 
			@Required(false) @Name("description") String description) throws Exception {
		// store the new location
		Map<String, Object> location = new HashMap<String, Object>();
		if (lat != null) {
			location.put("lat", lat);
		}
		if (lng != null) {
			location.put("lng", lng);
		}
		if (description != null) {
			location.put("description", description);
		}
		getContext().put("location", location);
		
		// trigger a change event
		ObjectNode params = JOM.createObjectNode();
		params.put("location", JOM.getInstance().convertValue(location, ObjectNode.class));
		trigger("change", params);
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
		String taskId = (String) getContext().get("taskId");
		if (taskId != null) {
			getScheduler().cancelTask(taskId);
			getContext().remove("taskId");
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
		getContext().put("taskId", taskId);
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
		return "The LocationAgent offers location information, " +
				"retrieved from a mobile phone. It can also simulate a location";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
