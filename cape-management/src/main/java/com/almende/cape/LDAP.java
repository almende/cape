package com.almende.cape;


import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;

public class LDAP  {
	private final static String HOST="localhost";
	private final static int PORT=389;

	private static LDAPConnection conn = null;
	
	public static LDAPConnection get() throws LDAPException{
		if (conn == null){
			conn = new LDAPConnection();
			conn.connect(HOST,PORT);
			//TODO: put password in a config item
			conn.bind(LDAPConnection.LDAP_V3,"cn=admin,dc=cape,dc=almende,dc=org", "admin4almende".getBytes());
		}
		return conn;
	}
}
