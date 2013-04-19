package com.almende.cape.agent;

import com.almende.cape.agent.intf.CapeGroupAgentIntf;

public abstract class CapeContactAgent extends CapeAgent implements CapeGroupAgentIntf {
	private static String DATA_TYPE = "group";
		
	@Override
	public void connect () throws Exception {
		super.connect();
		register(DATA_TYPE, null);
	}
	
	@Override
	public void disconnect () throws Exception {
		unregister(DATA_TYPE);
		super.disconnect();
	}
}
