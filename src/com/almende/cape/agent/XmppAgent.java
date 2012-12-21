package com.almende.cape.agent;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.context.Context;
import com.almende.eve.json.annotation.Name;
import com.almende.eve.service.xmpp.XmppService;

/**
 * The abstract class XmppAgent contains methods to set a password and 
 * connect/disconnect from xmpp.
 * The agents id is used as xmpp username.
 */
public abstract class XmppAgent extends Agent {
	/**
	 * Set the password for the agent.
	 * Only applicable when no password has been set (in that case, use
	 * updatePassword instead).
	 * @param password
	 * @throws Exception
	 */
	public void setPassword(@Name("password") String password) throws Exception {
		updatePassword(null, password);
	}
	
	/**
	 * Update the current password.
	 * @param oldPassword
	 * @param newPassword
	 * @throws Exception
	 */
	public void updatePassword(@Name ("oldPassword")  String oldPassword,
			@Name("newPassword") String newPassword) throws Exception {
		// TODO: do not store password!
		Context context = getContext();
		String password = (String) context.get("password");
		if (password != null) {
			if (!password.equals(oldPassword)) {
				throw new Exception("Cannot update password: " +
						"provided old password does not match.");
			}
		}
		getContext().put("password", newPassword);
	}
	
	/**
	 * Connect the agent to the xmpp server
	 * @throws Exception
	 */
	// TODO: agent must automatically connect on server startup 
	public void connect() throws Exception {
		AgentFactory factory = getAgentFactory();
		XmppService service = (XmppService) factory.getService("xmpp");
		if (service != null) {
			String username = getId();
			String password = (String) getContext().get("password");
			if (password == null) {
				throw new Exception(
					"Cannot connect: no password set for user '" + username + "'. " +
					"Please set a password using the method setPassword first.");
			}
			service.connect(getId(), username, password);
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
	
	/**
	 * Disconnect the agent from the xmpp server
	 * @throws Exception
	 */
	public void disconnect() throws Exception {
		AgentFactory factory = getAgentFactory();
		XmppService service = (XmppService) factory.getService("xmpp");
		if (service != null) {
			service.disconnect(getId());
		}
		else {
			throw new Exception("No XMPP service registered");
		}
	}
}
