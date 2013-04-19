package com.almende.cape.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Group implements Serializable {
	
	private static final long serialVersionUID = -3155415704786247870L;
	private String id=null;
	private String name=null;
	private Set<String> members=null;
	
	public Group() {
		this(UUID.randomUUID().toString(), "");
	}
	
	public Group(String name) {
		this(UUID.randomUUID().toString(), name);
	}
	
	public Group(String id, String name) {
		this.id = id;
		this.name = name;
		this.members = new HashSet<String>();
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@JsonIgnore
	public Set<String> getMembers() {
		return members;
	}
	
	public void addMembers(Set<String> memberURLs) {
		// TODO: check for doubles
		
		members.addAll(memberURLs);
	}
	
	public void addMember(String memberURL) throws Exception {	
		if(members.contains(memberURL))
			throw new Exception("Member already in group");
		
		members.add(memberURL);
	}
	
	public void removeMember(String memberURL) throws Exception {	
		if(!members.contains(memberURL))
			throw new Exception("Member is not in group");
		
		members.remove(memberURL);
	}	
}