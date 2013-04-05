package com.almende.cape.entity;

import java.util.Set;


public class Message {
	
	public Message(){}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public Set<String> getReceiver() {
		return receiver;
	}
	public void setReceiver(Set<String> receiver) {
		this.receiver = receiver;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public long getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}
	public String getAgent() {
		return agent;
	}
	public void setAgent(String agent) {
		this.agent = agent;
	}
	public String getBox() {
		return box;
	}
	public void setBox(String box) {
		this.box = box;
	}

	private String id = null;
	private String subject = null;
	private String message = null;
	private String sender = null;
	private Set<String> receiver = null;
	
	private String state = null;
	
	private long creationTime = 0;
	
	private String agent = null;
	private String box = null;
}
