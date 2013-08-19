package com.almende.cape.agent.intf;

import com.almende.eve.agent.AgentInterface;

public interface CapePersonalAgentIntf extends AgentInterface {
	public String getName();
	public String getPhone();
	public String getEmail();
	public String getPincode();
	String getBestPhoneAddress();
	void setLastAvailCall(long timestamp);
}
