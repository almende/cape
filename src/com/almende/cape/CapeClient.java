package com.almende.cape;

import java.util.Scanner;

import com.almende.cape.agent.CapeClientAgent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.MemoryContextFactory;
import com.almende.eve.service.http.HttpService;
import com.almende.eve.service.xmpp.XmppService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CAPE Client interface
 * @author jos
 */
public class CapeClient {
	private static String CONTACT_AGENT_URL = 
			"http://10.10.1.118:8080/Cape_Agents/agents/contactagent1/";
	
	public static void main(String[] args) {
		try {
			// TODO: request username and password in console
			CapeClient cape = new CapeClient();
			cape.login("alex", "alex");

			System.out.println("Press ENTER to quit");
			Scanner scanner = new Scanner(System.in);
	        scanner.nextLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor
	 * @throws Exception
	 */
	public CapeClient() {
		// TODO: read configuration from config file
		factory = new AgentFactory();
		
		factory.setContextFactory(new MemoryContextFactory(factory));

		String host = "ec2-54-246-112-19.eu-west-1.compute.amazonaws.com";
		Integer port = 5222;
		XmppService xmppService = new XmppService(factory);
		xmppService.init(host, port, null);
		factory.addService(xmppService);
		
		HttpService httpService = new HttpService(factory);
		httpService.init("http://localhost:8080/fake");
		factory.addService(httpService);
	}
	
	/**
	 * Login to CAPE
	 * @param username
	 * @param password
	 * @throws Exception 
	 */
	public void login(String username, String password) throws Exception {
		// create a user agent if not existing
		agent = (CapeClientAgent) factory.getAgent(username);
		if (agent == null) {
			agent = (CapeClientAgent) factory.createAgent(CapeClientAgent.class, username);
			agent.setContactAgent(CONTACT_AGENT_URL);
		}
		
		// connect to xmpp server
		XmppService xmppService = (XmppService) factory.getService("xmpp");
		if (xmppService == null) {
			throw new Exception("No XMPP Service configured.");
		}
		xmppService.connect(username, username, password);
		
		// copy current username
		this.username = username;
	}
	
	/**
	 * Logout from CAPE
	 * @throws Exception 
	 */
	public void logout() throws Exception {
		if (factory == null || username == null) {
			return;
		}
		
		// disconnect from xmpp service
		XmppService xmppService = (XmppService) factory.getService("xmpp");
		if (xmppService == null) {
			throw new Exception("No XMPP Service configured.");
		}
		xmppService.disconnect(username);		
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
	
	private String username = null;
	private CapeClientAgent agent = null;
	private AgentFactory factory = null;
}
