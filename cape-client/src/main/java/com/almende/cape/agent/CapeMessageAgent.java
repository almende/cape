package com.almende.cape.agent;

import com.almende.cape.agent.intf.CapeMessageAgentIntf;

public abstract class CapeMessageAgent extends CapeAgent implements CapeMessageAgentIntf {
	private static String DATA_TYPE = "message";

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
