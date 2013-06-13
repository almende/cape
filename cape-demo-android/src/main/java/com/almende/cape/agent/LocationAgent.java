package com.almende.cape.agent;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import com.almende.cape.android.R;
import com.almende.cape.entity.timeline.Slot;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocationAgent extends CapeStateAgent {
	/** Android Application State, not Eve context! */
	private static String BUILDING_URL = "xmpp:almende@openid.almende.org";
	LocationManager locationManager = null;

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location
			// provider.
			try {
				setLocation(location.getLatitude(), location.getLongitude(),
						"Updated from sensors...");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};
	private static TextView locationLabel = null;

	public void startSensor() throws Exception {
		stopSimulation();
		Activity context = (Activity) getState().get("AppContext");
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		locationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 0, 0, locationListener);
		Location location = locationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		try {
			setLocation(location.getLatitude(), location.getLongitude(),
					"Initial fix from sensor...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopSensor() {
		if (locationManager != null && locationListener != null){
			locationManager.removeUpdates(locationListener);
		}
	}

	/**
	 * Change the current location
	 * 
	 * @param lat
	 * @param lng
	 * @param description
	 * @throws Exception
	 */
	public void setLocation(@Required(false) @Name("lat") Double lat,
			@Required(false) @Name("lng") Double lng,
			@Required(false) @Name("description") String description)
			throws Exception {
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
		//getState().put("location", location);

		// trigger a change event
		ObjectNode params = JOM.createObjectNode();
		params.put("location",
				JOM.getInstance().convertValue(location, ObjectNode.class));
		getEventsFactory().trigger("change", params);
		
		// just push the location to the BuildingAgent
		// TODO: replace by using event subscription
		ObjectNode changeParams = JOM.createObjectNode();
		changeParams.put("subscriptionId", (String)null);
		changeParams.put("agent", getXmppUrl());
		changeParams.put("event", "change");
		changeParams.put("params", params);
		send(URI.create(BUILDING_URL), "onChange", changeParams);
		
		Activity act = (Activity) getState().get("AppContext");
		if (act != null) {
			act.runOnUiThread(new MyRunnable("Location:"+lat + ":" + lng + " - "+description, act));
			logger.info("Set location:"+lat+":"+lng+"::"+description);	
		} else {
			logger.severe("Activity not found!");
		}
	}

	private static class MyRunnable implements Runnable {
		private final String message;
		MyRunnable(final String message,Activity context) {
			this.message = message;
			locationLabel = (TextView) context.findViewById(R.id.location);
		}

		public void run() {
			if (locationLabel != null){
				locationLabel.setText(message);
			} else {
				logger.severe("LocationLabel is null???");
			}
		}
	}
	/**
	 * Start simulation of the location
	 */
	public void startSimulation() {
		stopSensor();
		onSimulate();
	}

	/**
	 * Stop simulation of the location
	 */
	public void stopSimulation() {
		String taskId = (String) getState().get("taskId");
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

		long delay = Math.round((10 * Math.random()) * 1000); // 0-10 sec
		JSONRequest request = new JSONRequest("onSimulate", null);
		String taskId = getScheduler().createTask(request, delay);
		getState().put("taskId", taskId);
	}

	/**
	 * Update the location based on a simple time based algorithm: The location
	 * will circle around Rotterdam once an hour.
	 */
	@SuppressWarnings("unchecked")
	public void updateLocation() {

		// Move from current location for a couple of meters towards the
		// north-west.....
		try {
			HashMap<String, Object> location = null;
			//TODO: default naar Almende's locatie.
			if (getState().containsKey("location")) {
				location = (HashMap<String, Object>) getState().get("location");
			}

			Double lat = (Double) location.get("lat");
			Double lng = (Double) location.get("lng");
			
			lat+=0.05;
			lng-=0.01;
			//
			// Calendar calendar = Calendar.getInstance();
			// double minutes = calendar.get(Calendar.MINUTE);
			// double seconds = calendar.get(Calendar.SECOND);
			//
			// // circle around Rotterdam once an hour
			// double t = (minutes + seconds / 60) / 60 * 2 * Math.PI;
			// Double lat = 51.9217 + 0.4 * Math.sin(t);
			// Double lng = 4.4811 + 0.4 * Math.cos(t);

			setLocation(lat, lng, "Simulated new location");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Register the user at the building agent
	 * @throws Exception
	 */
	public void registerBuilding() throws Exception {
		String userId = (String) getState().get("xmppUsername");
		logger.info("registering userId " + userId + " at building agent...");
		if (userId != null) {
			// register our use at the building agent
			ObjectNode params = JOM.createObjectNode();
			params.put("userId", userId);
			logger.info("registered at building agent");
//			send(BUILDING_URL, "registerUser", params); 
			// FIXME: bug in Eve-XMPP synchronous with cascaded calls
		}
		else {
			System.err.println("Couldn't find the userId?");
		}		
	}
	
	/**
	 * Unregister our user from the building agent
	 * @throws Exception
	 */
	public void unregisterBuilding() throws Exception {
		String userId = (String) getState().get("xmppUsername");
		logger.info("unregistering userId " + userId + " at building agent...");
		if (userId != null) {
			// register our use at the building agent
			ObjectNode params = JOM.createObjectNode();
			params.put("userId", userId);
			
			logger.info("unregistered at building agent");
//			send(BUILDING_URL, "unregisterUser", params); 
			// FIXME: bug in Eve-XMPP synchronous with cascaded calls
		}
		else {
			// TODO: warning or error
		}
	}

	@Override
	public String getDescription() {
		return "The LocationAgent offers location information, "
				+ "retrieved from a mobile phone. It can also simulate a location";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

    private static Logger logger = Logger.getLogger(LocationAgent.class.getSimpleName());

	@Override
	public boolean setSlot(long startTime, long endTime, String desc,
			String occurence) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<Slot> getSlots(long startTime, long endTime,
			String occurence) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Slot> getSlotsCombined(long startTime, long endTime) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Slot getCurrentSlot(String occurence) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Slot getCurrentSlotCombined() {
		// TODO Auto-generated method stub
		return null;
	};
}
