package com.almende.cape.agent.intf;

import java.util.Set;

import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;
import com.almende.cape.entity.Group;

public interface CapeGroupAgentIntf extends AgentInterface {
	public Group createGroup(@Name("name") String name) throws Exception;
	public Group updateGroup(@Name("id") String groupId, @Name("group") Group newGroup) throws Exception;
	public void removeGroup(@Name("id") String groupId) throws Exception;
	public Set<Group> getGroups();
	public Group getGroup(@Name("id") String id) throws Exception;
	public Set<String> getGroupMembers(@Name("groupId") String groupId) throws Exception;
	public Set<String> getAllMembers() throws Exception;
	public void addMembers(@Name("groupId") String groupId, @Name("agentIds") Set<String> agentIds) throws Exception;
	public void addMember(@Name("groupId") String groupId, @Name("agentId") String agentId) throws Exception;
	public void removeMember(@Name("groupId") String groupId, @Name("agentId") String agentId) throws Exception;
	public void removeMemberFromAllGroups(@Name("agentId") String agentId);
}
