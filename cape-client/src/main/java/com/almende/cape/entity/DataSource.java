package com.almende.cape.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class DataSource implements Serializable {
	public DataSource() {}
	
	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	public String getDirection() {
		return direction;
	}
	
	public void setDirection(String direction) {
		this.direction = direction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}

		DataSource other = (DataSource) obj;
		return userId.equals(other.userId) && 
				agentUrl.equals(other.agentUrl) &&
				dataType.equals(other.dataType);
	}

	private String userId = null;
	private String agentUrl = null;
	private String dataType = null;
	private String direction = null;
}
