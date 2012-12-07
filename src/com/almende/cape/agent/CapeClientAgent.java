package com.almende.cape.agent;

import com.almende.eve.agent.Agent;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.json.annotation.Required;
import com.almende.eve.json.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CapeClientAgent extends Agent {
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
		String contactAgentUrl = (String) getContext().get("contactAgentUrl");
		if (contactAgentUrl == null) {
			throw new Exception("No contactAgentUrl set. " +
					"Set this url first using the method setContactAgent(url)");
		}
		
		String method = "getContacts";
		ObjectNode params = JOM.createObjectNode();
		String filterStr = (filter != null) ? JOM.getInstance().writeValueAsString(filter) : "";
		params.put("filter", filterStr);
		String contacts = send(contactAgentUrl, method, params, String.class);
		ArrayNode array = JOM.getInstance().readValue(contacts, ArrayNode.class);
		
		return array;
	}

	/**
	 * Set the url for the contacts agent
	 * @param url
	 */
	public void setContactAgent(@Name("url") String url) {
		getContext().put("contactAgentUrl", url);
	}
	
	/**
	 * Return the currently set url of the contact agent
	 * @return url
	 */
	public String getContactAgent() {
		return (String) getContext().get("contactAgentUrl");
	}
	
	@Override
	public String getDescription() {
		return "CAPE Client Agent";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}
}
