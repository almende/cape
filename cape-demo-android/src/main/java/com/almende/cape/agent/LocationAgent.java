package com.almende.cape.agent;

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
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.rpc.jsonrpc.JSONRequest;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class LocationAgent extends CapeStateAgent {
	/** Android Application Context, not Eve context! */
	private static String BUILDING_URL = "xmpp:building@openid.almende.org";
	static Activity context = null;
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

	public void setAndroidContext(Activity context) {
		LocationAgent.context = context;
	}

	public void startSensor() throws Exception {
		stopSimulation();
		
		if (LocationAgent.context == null) {
			throw new Exception("Android App context is not yet set!");
		}
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
	 * Get the current state
	 * 
	 * @param state
	 *            Available values: "location"
	 * @return
	 * @throws Exception
	 */
	@Override
	public Object getState(@Name("state") String state) throws Exception {
		if (state.equals("location")) {
			return getContext().get("location");
		} else {
			// no information available for other states
			throw new Exception("No information available for state '" + state
					+ "', " + "only information for 'location' is available.");
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
		getContext().put("location", location);

		// trigger a change event
		ObjectNode params = JOM.createObjectNode();
		params.put("location",
				JOM.getInstance().convertValue(location, ObjectNode.class));
		trigger("change", params);
		
		Activity act = (Activity) context;
		if (act != null) {
			act.runOnUiThread(new MyRunnable("Location:"+lat + ":" + lng + " - "+description));
			logger.info("Set location:"+lat+":"+lng+"::"+description);	
		} else {
			logger.severe("Activity not found!");
		}
	}

	private static class MyRunnable implements Runnable {
		private final String message;
		MyRunnable(final String message) {
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

		long delay = Math.round((10 * Math.random()) * 1000); // 0-10 sec
		JSONRequest request = new JSONRequest("onSimulate", null);
		String taskId = getScheduler().createTask(request, delay);
		getContext().put("taskId", taskId);
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
			if (getContext().containsKey("location")) {
				location = (HashMap<String, Object>) getContext().get("location");
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
		String userId = (String) getContext().get("xmppUsername");
		logger.info("registering userId " + userId + " at building agent...");
		if (userId != null) {
			// register our use at the building agent
			ObjectNode params = JOM.createObjectNode();
			params.put("userId", userId);
//			send(BUILDING_URL, "registerUser", params);
			
			logger.info("registered at building agent");
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
		String userId = (String) getContext().get("xmppUsername");
		logger.info("unregistering userId " + userId + " at building agent...");
		if (userId != null) {
			// register our use at the building agent
			ObjectNode params = JOM.createObjectNode();
			params.put("userId", userId);
//			send(BUILDING_URL, "unregisterUser", params);
		
			logger.info("unregistered at building agent");
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

    private static Logger logger = Logger.getLogger(LocationAgent.class.getSimpleName());;
}
