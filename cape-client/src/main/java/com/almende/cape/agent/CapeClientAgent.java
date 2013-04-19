package com.almende.cape.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.almende.cape.agent.intf.CapeGroupAgentIntf;
import com.almende.cape.agent.intf.CapeMessageAgentIntf;
import com.almende.cape.agent.intf.CapeStateAgentIntf;
import com.almende.cape.entity.DataSource;
import com.almende.cape.entity.Group;
import com.almende.cape.entity.Message;
import com.almende.cape.entity.timeline.Slot;
import com.almende.cape.handler.MessageHandler;
import com.almende.cape.handler.StateChangeHandler;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.agent.annotation.Required;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CapeClientAgent extends CapeAgent {
	public Set<Group> getGroups() throws Exception {
		CapeGroupAgentIntf ca = getContactAgent();
		return ca.getGroups();
	}
	
	public void createGroup(@Name("name") String name) throws Exception {
		CapeGroupAgentIntf ca = getContactAgent();
		ca.createGroup(name);
	}
	
	public void sendMessage(String message, String subject, Set<String> receivers) throws Exception {
		CapeMessageAgentIntf messageAgent = getMessageAgent();
		messageAgent.sendMessage(message, subject, receivers);
	}
	
	public void setMessageHandler(MessageHandler handler) {
		messageHandlers.put(getId(), handler);
	}
	

	public void onMessage(@Name("message") Message message) throws Exception {
		MessageHandler handler = messageHandlers.get(getId());
		if (handler != null) {
			handler.onMessage(message);			
		}
		else {
			throw new Exception ("Cannot deliver message: " +
					"no message handler set.");
		}
	}
	
	public List<Slot> getSlots(@Name("start") long startTime, @Name("end") long endTime, @Name("occurence") String occurence) throws Exception {
		CapeStateAgentIntf stateAgent = getStateAgent();
		return stateAgent.getSlots(startTime, endTime, occurence);
	}
	
	/**
	 * Remove the currently attached message handler.
	 * @throws Exception 
	 */
	public void removeMessageHandler() throws Exception {
		if (messageHandlers.containsKey(getId())) {
			messageHandlers.remove(getId());
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
		List<DataSource> dataSources = findDataSource(userId, "state");
		String agentUrl = dataSources.get(0).getAgentUrl();
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
	
	private CapeMessageAgentIntf getMessageAgent() throws Exception {
		String url = getMessageAgentUrl();
		if(url==null)
			throw new Exception("No message agent found");
		return getAgentFactory().createAgentProxy(null, url, CapeMessageAgentIntf.class);
	}
	
	private String getMessageAgentUrl() throws Exception {
		String messageAgentURL = (String) getState().get("messageAgent");
		if(messageAgentURL==null) {
			List<DataSource> dataSources = findDataSource(getId(), "message");
			for(DataSource dataSource : dataSources) {
				if(dataSource.getDirection().equals("producer")) {
					getState().put("messageAgent", dataSource.getAgentUrl());
					return dataSource.getAgentUrl();
				}
			}
		}
		return messageAgentURL;
	}
	
	private CapeGroupAgentIntf getContactAgent() throws Exception {
		String url = getContactAgentUrl();
		if(url==null)
			throw new Exception("No group agent found");
		return getAgentFactory().createAgentProxy(null, url, CapeGroupAgentIntf.class);
	}
	
	private String getContactAgentUrl() throws Exception {
		String contactAgentURL = (String) getState().get("contactAgent");
		if(contactAgentURL==null) {
			List<DataSource> dataSources = findDataSource(getId(), "group");
			logger.info("Search for ds:group from "+getId()+" and found: "+dataSources.size());
			for(DataSource dataSource : dataSources) {
				if(dataSource.getDirection().equals("producer")) {
					getState().put("contactAgent", dataSource.getAgentUrl());
					return dataSource.getAgentUrl();
				}
			}
		}
		return contactAgentURL;
	}
	
	private CapeStateAgentIntf getStateAgent() throws Exception {
		String url = getStateAgentUrl();
		if(url==null)
			throw new Exception("No state agent found");
		return getAgentFactory().createAgentProxy(null, url, CapeStateAgentIntf.class);
	}
	
	private String getStateAgentUrl() throws Exception {
		String contactAgentURL = (String) getState().get("stateAgent");
		if(contactAgentURL==null) {
			List<DataSource> dataSources = findDataSource(getId(), "state");
			for(DataSource dataSource : dataSources) {
				if(dataSource.getDirection().equals("producer")) {
					getState().put("stateAgent", dataSource.getAgentUrl());
					return dataSource.getAgentUrl();
				}
			}
		}
		return contactAgentURL;
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
	private static Map<String, MessageHandler> messageHandlers = 
			new ConcurrentHashMap<String, MessageHandler>();
	private static Map<String, StateSubscription> stateChangeHandlers = 
			new ConcurrentHashMap<String, StateSubscription>();
	
	private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
}
