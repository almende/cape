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
	 * Register a new user to the building
	 * @param agentUrl
	 * @throws Exception
	 */
	public void registerUser(@Name("AgentUrl") String agentUrl) throws Exception {
		// unregister first to prevent double entries
		unregisterUser(agentUrl);
		
		// subscribe to the agents change location event.
		String subscriptionId = subscribe(agentUrl, "change", "onChange");

		// create a new user
		Person user = new Person();
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
	}

	/**
	 * Unregister an existing user from the building
	 * @param agentUrl
	 * @throws Exception
	 */
	public void unregisterUser(@Name("AgentUrl") String agentUrl) throws Exception{
		List<Person> removedUsers = new ArrayList<Person>(); 
		
		// add the user to the list with registered users
		Context context = getContext();
		List<Person> users = (List<Person>) context.get("users");
		if (users != null) {
			for (Person user : users) {
				if (user.agentUrl.equals(agentUrl)) {
					removedUsers.add(user);
					users.remove(user);
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
				List<Person> presentUsers = new ArrayList<Person>();
				for (Person user : users) {
					if (user.present) {
						presentUsers.add(user);
					}
				}
				return presentUsers;
			}
			else if ("out".equals(status)) {
				List<Person> absentUsers = new ArrayList<Person>();
				for (Person user : users) {
					if (!user.present) {
						absentUsers.add(user);
					}
				}
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
			for (Person user : users) {
				if (user.agentUrl.equals(agent)) {
					// found the correct user. update its location
					if (params.has("location")) {
						user.location = new Location((ObjectNode) params.get("location"));
						if (user.location != null) {
							// TODO: calculate whether the user is in the building or not
						}
					}
				}
			}
		}
	}
}