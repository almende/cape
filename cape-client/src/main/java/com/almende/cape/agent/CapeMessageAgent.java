package com.almende.cape.agent;

import com.almende.cape.entity.Message;
import com.almende.eve.agent.annotation.Name;

public abstract class CapeMessageAgent extends CapeAgent {
	private static String DATA_TYPE = "message";
	
	public abstract void onMessage(@Name("message") Message message) throws Exception;

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
