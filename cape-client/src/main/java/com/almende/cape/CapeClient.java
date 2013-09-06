package com.almende.cape;

import java.util.Set;

import com.almende.cape.agent.CapeClientAgent;
import com.almende.cape.entity.Group;
import com.almende.cape.handler.MessageHandler;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.state.MemoryStateFactory;
import com.almende.eve.transport.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CAPE Client interface
 */
public class CapeClient {
	private CapeClientAgent agent = null;
	private AgentHost factory = null;
	
	/**
	 * Constructor
	 * @throws Exception
	 */
	public CapeClient() {
		this(AgentHost.getInstance());
	}
	public CapeClient( AgentHost factory ){
		this( factory, "openid.ask-cs.com" );
	}
	public CapeClient( AgentHost factory, String host ) {
		this( factory, host, 5222 );
	}

	public CapeClient( AgentHost factory, String host, int port ) {
		if (factory == null) {
			try {
				factory = AgentHost.getInstance();
				factory.setStateFactory(new MemoryStateFactory());
				factory.setSchedulerFactory(new RunnableSchedulerFactory(factory, ".runnablescheduler"));
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Failed to init factory!" + e.getMessage());
			}
		}
		String service = host;
		XmppService xmppService = new XmppService(factory,host, port, service);
		factory.addTransportService(xmppService);
		this.factory=factory;
	}

	/**
	 * Login to CAPE
	 * @param username
	 * @param password
	 * @throws Exception 
	 */
	public void login(String username, String password) throws Exception {
		// ensure we are logged out
		logout();
		
		// create a user agent if not existing
		CapeClientAgent agent = (CapeClientAgent) factory.getAgent(username);
		if (agent == null) {
			System.out.println("Creating new agent");
			agent = (CapeClientAgent) factory.createAgent(
					CapeClientAgent.class, username);
		}
		
		String resource = "client";
		agent.setAccount(username, password, resource);
		
		// connect to xmpp service
		agent.connect();
		
		// no exceptions thrown
		this.agent = agent;
	}
	
	/**
	 * Logout from CAPE
	 * @throws Exception 
	 */
	public void logout() throws Exception {
		if (agent != null) {
			// remove any handlers
			agent.removeMessageHandler();
			
			// disconnect from xmpp service
			agent.disconnect();

			agent = null;
		}
	}
	
	public void onMessage(MessageHandler messageHandler) throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		agent.setMessageHandler(messageHandler);
	}
	
	/**
	 * Retrieve contacts
	 * @param filter
	 * @return contacts
	 * @throws Exception 
	 */
	public ArrayNode getContacts(ObjectNode filter) throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		return agent.getContacts(filter);
	}
	
	public Set<Group> getGroups() throws Exception {
		if (agent == null) {
			throw new Exception("Not logged in");
		}
		System.out.println("AgentId: "+agent.getId());
		return agent.getGroups();
	}
	
	@Override
	public void finalize () {
		try {
			logout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

}
