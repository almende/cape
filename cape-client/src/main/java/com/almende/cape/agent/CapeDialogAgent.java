package com.almende.cape.agent;

import com.almende.eve.rpc.annotation.Name;

public abstract class CapeDialogAgent extends CapeAgent {
	private static String DATA_TYPE = "dialog";
	
	public abstract void onNotification(@Name("message") String message) throws Exception;

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
