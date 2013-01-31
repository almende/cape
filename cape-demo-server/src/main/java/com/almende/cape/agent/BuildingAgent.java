package com.almende.cape.agent;

import java.util.ArrayList;
import java.util.List;

import com.almende.cape.entity.Location;
import com.almende.cape.entity.Person;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.context.Context;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

	@Override
	public void create () {
		super.create();
		
		// TODO: do not hardcode location and account
		try {
			// set location to Almende BV
			setLocation(51.908978, 4.479646);

			// set default account to building
			setAccount("building", "building", null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the buildings location
	 * @param lat
	 * @param lng
	 */
	public void setLocation(@Name("lat") Double lat, @Name("lng") Double lng) {
		Location location = new Location(lat, lng);
		getContext().put("location", location);
	}
	
	/**
	 * Retrieve the buildings location. Returns null if no location is set.
	 * @return location. An object containing parameters lat and lng
	 */
	public Location getLocation() {
		return (Location) getContext().get("location");
	}
	
	/**
	 * Register a user to the building
	 * @param userId
	 * @return user
	 * @throws Exception
	 */
	public Person registerUser(@Name("userId") String userId) throws Exception {
		// unregister first to prevent double entries
		unregisterUser(userId);
		
		String dataType = "state";
		String agentUrl = findDataSource(userId, dataType);
		if (agentUrl == null) {
			throw new Exception("No agent found providing state information " +
					"for user " + userId);
		}
		
		// subscribe to the agents change location event.
		String subscriptionId = subscribe(agentUrl, "change", "onChange");

		// create a new user
		Person user = new Person();
		user.userId = userId;
		user.agentUrl = agentUrl;
		user.subscriptionId = subscriptionId;
		
		// add the user to the list with registered users
		Context context = getContext();
		List<Person> users = (List<Person>) context.get("users");
		if (users == null) {
			users = new ArrayList<Person>();
		}	
		users.add(user);
		context.put("users", users);
		
		return user;
	}

	/**
	 * Unregister an existing user from the building
	 * @param userId
	 * @throws Exception
	 */
	public void unregisterUser(@Name("userId") String userId) throws Exception{
		List<Person> removedUsers = new ArrayList<Person>(); 
		
		// add the user to the list with registered users
		Context context = getContext();
		List<Person> users = (List<Person>) context.get("users");
		if (users != null) {
			int i = 0;
			while (i < users.size()) {
				Person user = users.get(i);
				if (user.userId.equals(userId)) {
					removedUsers.add(user);
					users.remove(user);
				}
				else {
					i++;
				}
			}
			context.put("users", users);
		}	

		for (Person user : removedUsers) {
			unsubscribe(user.agentUrl, "change", "onChange");
		}
	}
	
	/**
	 * Retrieve a list with all registered users and their status
	 * @param status.   Optional parameter to filter users.
	 *                  Available values: "in" or "out"
	 * @return users
	 */
	public List<Person> list(@Required(false) @Name("status") String status) {
		List<Person> users = (List<Person>) getContext().get("users");
		if (users != null) {
			if ("in".equals(status)) {
				Boolean present = true;
				List<Person> presentUsers = filter(users, present);
				return presentUsers;
			}
			else if ("out".equals(status)) {
				Boolean present = false;
				List<Person> absentUsers = filter(users, present);
				return absentUsers;
			}
			else {
				return users;
			}
		}
		else {
			return new ArrayList<Person>();
		}
	}
	
	/**
	 * Filter given list with users by status: present or absent
	 * @param users
	 * @param present
	 * @return filteredUsers
	 */
	private List<Person> filter(List<Person> users, Boolean present) {
			List<Person> filteredUsers = new ArrayList<Person>();
			for (Person user : users) {
				if (present.equals(user.present)) {
					filteredUsers.add(user);
				}
			}
			return filteredUsers;
	}
	
	
	/**
	 * Callback method for the change event of LocationAgents. 
	 * @param subscriptionId
	 * @param agent
	 * @param event
	 * @param params      params is supposed to contain an object location,
	 *                    containing lat and lng (both numbers)
	 * @throws Exception
	 */
	public void onChange(
			@Required(false) @Name("subscriptionId") String subscriptionId, 
			@Required(false) @Name("agent") String agent, 
			@Required(false) @Name("event") String event, 
			@Required(false) @Name("params") ObjectNode params) throws Exception {
		// trigger a change event (for debugging)
		ObjectNode changeParams = JOM.createObjectNode();
		changeParams.put("subscriptionId", subscriptionId);
		changeParams.put("agent", agent);
		changeParams.put("event", event);
		changeParams.put("params", params);
		trigger("change", changeParams);
		
		// find the user from the list, and update its location
		Context context = getContext();
		List<Person> users = (List<Person>) context.get("users");
		if (users != null) {
			// calculate the number of present users
			List<Person> presentUsers = filter(users, true);
			int userCountBefore = presentUsers.size();
			
			// find this user to update its data
			for (Person user : users) {
				if (user.agentUrl.equals(agent)) {
					// found the correct user. update its location
					if (params.has("location")) {
						user.location = new Location((ObjectNode) params.get("location"));

						Location buildingLocation = (Location) context.get("location");
						if (buildingLocation != null && user.location != null) {
							// calculate distance to building
							user.distance = distance(buildingLocation, user.location);
							
							// calculate if within range of building
							Double range = 0.1; // km
							user.present = withinRange(buildingLocation, user.location, range);
							
							context.put("users", users);
						}
					}
				}
			}
			
			// check whether the number of present users changed to 1
			presentUsers = filter(users, true);
			int userCountAfter = presentUsers.size();
			if (userCountBefore > 2 && userCountAfter == 1) {
				Person lastUser = presentUsers.get(0);
				notifyLastUser(lastUser.userId);
			}
		}
	}
	
	/**
	 * Send a notification to the last user
	 */
	// TODO: change notifyLastUser to private method
	public void notifyLastUser(@Name("userId") String userId) {
		try {
			String message = 
					"Hello " + userId + ". You are the last one in the building. " +
					"Please don't forget to lock up everyting properly when you leave. " +
				    "Thanks, your BuildingAgent.";
			sendNotification(userId, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check whether two locations with a given range of each other
	 * @param a
	 * @param b
	 * @param range   in kilometers
	 * @return
	 */
	private boolean withinRange(Location a, Location b, Double range) {
		Double distance = distance(a, b);
		return distance < range;
	}
	
	/**
	 * Calculate the distance between two location
	 * http://www.movable-type.co.uk/scripts/latlong.html
	 * @param a
	 * @param b
	 * @return distance in km
	 */
	private Double distance(Location a, Location b) {
		Double R = (double) 6371; // km
		Double dLat = Math.toRadians(b.lat - a.lat);
		Double dLon = Math.toRadians(b.lng - a.lng);
		Double lat1 = Math.toRadians(a.lat);
		Double lat2 = Math.toRadians(b.lat);

		Double h = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
		Double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1-h)); 
		Double d = R * c;
		
		return d;
	}	
}