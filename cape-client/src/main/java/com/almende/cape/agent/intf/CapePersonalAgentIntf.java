package com.almende.cape.agent.intf;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;

public interface CapePersonalAgentIntf extends AgentInterface {
	public String getName();
	public String getPhone();
	public String getEmail();
	public String getPincode();
	String getBestPhoneAddress();
	void setLastAvailCall( @Name("timestamp") long timestamp);

	public String getMessageAgentURL();

	public void setResource(@Name("key") String key, @Name("value")  String value);
	public String getResource(@Name("key") String key);
	
	public String getDomainAgent();
}
