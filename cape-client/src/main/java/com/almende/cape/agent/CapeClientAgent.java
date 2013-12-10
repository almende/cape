package com.almende.cape.agent;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.cape.handler.NotificationHandler;
import com.almende.cape.handler.StateChangeHandler;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Optional;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CapeClientAgent extends CapeDialogAgent {
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
		URI agentUrl = findDataSource(userId, "state");
		if (agentUrl == null) {
			throw new Exception("No agent found providing state information for " +
					"user " + userId);
		}
		logger.info("Found agent providing this state, url=" + agentUrl + ". subscribing...");
		String subscriptionId = getEventsFactory().subscribe(agentUrl, "change", "onStateChange");

		StateSubscription subscription = new StateSubscription();
		subscription.subscriptionId = subscriptionId;
		subscription.handler = handler;
		subscription.agentUrl = agentUrl.toASCIIString();
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
				getEventsFactory().unsubscribe(URI.create(subscription.agentUrl), subscription.subscriptionId);
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
			getEventsFactory().unsubscribe(URI.create(subscription.agentUrl), subscription.subscriptionId);
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
			@Optional @Name("subscriptionId") String subscriptionId,
			@Optional @Name("agent") String agent,
	        @Optional @Name("event") String event, 
	        @Optional @Name("params") ObjectNode params) {
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
