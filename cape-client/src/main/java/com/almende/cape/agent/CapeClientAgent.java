package com.almende.cape.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.cape.handler.NotificationHandler;
import com.almende.cape.handler.StateChangeHandler;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CapeClientAgent extends CapeDialogAgent {
	/**
	 * Retrieve contacts
	 * @param contactFilter
	 * @return contacts
	 * @throws Exception 
	 */
	// TODO: replace arrayNode for List<Contact> and ObjectNode with java class Contact?
	public ArrayNode getContacts(
			@Name("contactFilter") @Required(false) ObjectNode filter) 
			throws Exception {
		String userId = getId();
		String dataType = "contacts";
		String contactAgentUrl = findDataSource(userId, dataType);
		if (contactAgentUrl == null) {
			throw new Exception(
					"No data source found containing contacts of user " + getId());
		}
		// TODO: cache the retrieved data source for some time
		
		String method = "getContacts";
		ObjectNode params = JOM.createObjectNode();
		String filterStr = (filter != null) ? JOM.getInstance().writeValueAsString(filter) : "";
		params.put("filter", filterStr);
		String contacts = send(contactAgentUrl, method, params, String.class);
		ArrayNode array = JOM.getInstance().readValue(contacts, ArrayNode.class);
		
		return array;
	}

	/**
	 * Send a notification to any user
	 * @param userId  can be null
	 * @param message
	 * @throws Exception 
	 */
	public void sendNotification(@Required(false) @Name("userId") String userId, 
			@Name("message") String message) throws Exception {
		if (userId == null) {
			userId = getId();
		}
		String dataType = "dialog";
		
		// find an agent which can handle a dialog with the user
		String notificationAgentUrl = findDataSource(userId, dataType);
		if (notificationAgentUrl == null) {
			throw new Exception(
					"No data source found supporting a dialog with user " + userId);
		}
		
		// send the notification
		String method = "onNotification";
		ObjectNode params = JOM.createObjectNode();
		params.put("message", message);
		send(notificationAgentUrl, method, params);
	}

	/**
	 * Attach a notification handler to the agent.
	 * The notification handler will be available as long as the agent is 
	 * instantiated, but is not persisted. 
	 * @param notificationHandler
	 * @throws Exception 
	 */
	public void setNotificationHandler(NotificationHandler handler) 
			throws Exception {
		notificationHandlers.put(getId(), handler);
	}

	/**
	 * Remove the currently attached notification handler.
	 * @throws Exception 
	 */
	public void removeNotificationHandler() throws Exception {
		if (notificationHandlers.containsKey(getId())) {
			notificationHandlers.remove(getId());
		}
	}
	
	/**
	 * Receive a notification and dispatch it to the attached notification 
	 * handler. If no notification handler is attached, an exception will be
	 * thrown.
	 * @param message
	 * @throws Exception 
	 */
	@Override
	public void onNotification(@Name("message") String message) throws Exception {
		NotificationHandler handler = notificationHandlers.get(getId());
		if (handler != null) {
			handler.onNotification(message);			
		}
		else {
			throw new Exception ("Cannot deliver notification: " +
					"no notification handler set.");
		}
	}

	/**
	 * 
	 * @param userId
	 * @param subscribedUserId
	 * @return
	 */
	private String getChangeHandlerKey(String userId, String subscribedUserId) {
		return userId + ";" + subscribedUserId;
	}
	
	/**
	 * Receive a notification and dispatch it to the attached notification 
	 * handler. If no notification handler is attached, nothing happens.
	 * @param message
	 * @throws Exception 
	 */
	public void addStateChangeHandler(String userId, String state, 
			StateChangeHandler handler) throws Exception {
		if (userId == null) {
			userId = getId();
		}

		String key = getChangeHandlerKey(getId(), userId);
		if (stateChangeHandlers.containsKey(key)) {
			throw new Exception ("A state change handler already exists for " +
					"userId=" + userId);
		}

		// TODO: should run a process which regularly refreshes the url for the datasource
		logger.info("Registering state handler for userId=" + userId + ", state=" + state);
		String agentUrl = findDataSource(userId, "state");
		if (agentUrl == null) {
			throw new Exception("No agent found providing state information for " +
					"user " + userId);
		}
		logger.info("Found agent providing this state, url=" + agentUrl + ". subscribing...");
		String subscriptionId = subscribe(agentUrl, "change", "onStateChange");

		StateSubscription subscription = new StateSubscription();
		subscription.subscriptionId = subscriptionId;
		subscription.handler = handler;
		subscription.agentUrl = agentUrl;
		stateChangeHandlers.put(key, subscription);

		logger.info("Registered state handler");
	}
	
	/**
	 * Remove a registered state change handler
	 * @param handler
	 * @throws Exception 
	 */
	public void removeStateChangeHandler(StateChangeHandler handler) throws Exception {
		for (String key : stateChangeHandlers.keySet()) {
			StateSubscription subscription = stateChangeHandlers.get(key);
			if (subscription.handler == handler) {
				unsubscribe(subscription.agentUrl, subscription.subscriptionId);
				stateChangeHandlers.remove(key);
			}
		}
	}
	
	/**
	 * Remove all registered state change handlers
	 * @throws Exception 
	 */
	public void removeStateChangeHandlers() throws Exception {
		for (String key : stateChangeHandlers.keySet()) {
			StateSubscription subscription = stateChangeHandlers.get(key);
			unsubscribe(subscription.agentUrl, subscription.subscriptionId);
			stateChangeHandlers.remove(key);
		}
	}
	
	/**
	 * Handle an state change trigger
	 * @param agent
	 * @param event
	 * @param params
	 */
	public void onStateChange(
			@Required(false) @Name("subscriptionId") String subscriptionId,
			@Required(false) @Name("agent") String agent,
	        @Required(false) @Name("event") String event, 
	        @Required(false) @Name("params") ObjectNode params) {
		for (String key : stateChangeHandlers.keySet()) {
			StateSubscription subscription = stateChangeHandlers.get(key);
			if (subscription.subscriptionId.equals(subscriptionId)) {
				subscription.handler.onChange(params);
			}
		}
	}
	
	@Override
	public String getDescription() {
		return "CAPE Client Agent";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
	
	/**
	 * Helper class to store state handlers
	 */
	private class StateSubscription {
		public String agentUrl;
		public String subscriptionId;
		public StateChangeHandler handler;
	}
	
	// TODO: the notification handler and stateChange handlers are a singleton 
	//       per user right now, as we cannot have a single, continuously 
	//       instantiated agent: the AgentFactory  will instantiate a new 
	//       instance of our agent on incoming calls.
	private static Map<String, NotificationHandler> notificationHandlers = 
			new ConcurrentHashMap<String, NotificationHandler>();
	private static Map<String, StateSubscription> stateChangeHandlers = 
			new ConcurrentHashMap<String, StateSubscription>();
	
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
