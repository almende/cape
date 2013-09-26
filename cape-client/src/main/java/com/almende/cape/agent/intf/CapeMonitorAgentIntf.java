package com.almende.cape.agent.intf;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;

public interface CapeMonitorAgentIntf extends AgentInterface {
	
	public int checkShortage(@Name("state") @Required(false) String state) throws Exception;
	
	public String getMonitorGroup();
}
