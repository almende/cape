package com.almende.cape.agent;

import com.almende.eve.rpc.annotation.Name;

public abstract class CapeContactAgent extends CapeAgent {
	private static String DATA_TYPE = "contacts";
	
	public abstract String getContacts(@Name("filter") String filter) throws Exception;
	
	@Override
	public void connect () throws Exception {
		super.connect();
		register(DATA_TYPE);
	}
	
	@Override
	public void disconnect () throws Exception {
		unregister(DATA_TYPE);
		super.disconnect();
	}
}
