package com.almende.cape.agent.intf;

import java.util.Set;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.annotation.Required;

public interface CapeDomainAgentIntf extends AgentInterface {
	public Set<String> findUser(@Name("name") @Required(false) String name,
			@Name("phone") @Required(false) String phone,
			@Name("email") @Required(false) String email,
			@Name("pincode") @Required(false) String pincode) throws Exception; 
}
