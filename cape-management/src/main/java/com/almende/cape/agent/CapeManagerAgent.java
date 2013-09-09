package com.almende.cape.agent;

import com.almende.cape.LDAP;
import com.almende.eve.agent.Agent;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;

@Access(AccessType.PUBLIC)
public class CapeManagerAgent extends Agent {
	public String registerAgent(@Name("username") String username,
			@Name("domain") String domain, @Name("password") String password,
			@Name("givenname") String givenname, @Name("surname") String surname)
			throws Exception {
		LDAPConnection conn = null;
		try {
			conn = LDAP.get();
		} catch (LDAPException e) {
			throw new Exception("Sorry, couldn't connect to LDAP server", e);
		}
		try {
			LDAPAttributeSet attr = new LDAPAttributeSet();
			attr.add(new LDAPAttribute("cn", username));
			attr.add(new LDAPAttribute("givenName", givenname));
			attr.add(new LDAPAttribute("sn", surname));
			attr.add(new LDAPAttribute("objectClass", "inetOrgPerson"));
			attr.add(new LDAPAttribute("objectClass", "organizationalPerson"));
			attr.add(new LDAPAttribute("objectClass", "person"));
			attr.add(new LDAPAttribute("objectClass", "top"));
			attr.add(new LDAPAttribute("userPassword", password));
			
			LDAPEntry entry = new LDAPEntry("uid=" + username + ",ou=" + domain
					+ "_Users,dc=cape,dc=almende,dc=org", attr);
			//TODO: update agent if exists?
			conn.add(entry);
		} catch (LDAPException e) {
			throw new Exception("Failed to add agent to LDAP", e);
		}
		return "";
	}

	public void forgetAgent(@Name("username") String username,
			@Name("domain") String domain) throws Exception {
		try {
			LDAPConnection conn = LDAP.get();
			conn.delete("uid=" + username + ",ou=" + domain
					+ "_Users,dc=cape,dc=almende,dc=org");
		} catch (LDAPException e) {
			throw new Exception("Sorry, couldn't connect to LDAP server", e);
		}
	}

	@Override
	public String getDescription() {
		return "This agent offers CAPE account management features for other agents";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

}
