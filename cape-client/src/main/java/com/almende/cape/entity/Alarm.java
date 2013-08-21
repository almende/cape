package com.almende.cape.entity;

import java.io.Serializable;

public class Alarm implements Serializable {
	
	private static final long serialVersionUID = 		2245233206565961054L;
	public static final String NO_ALARM_RESPONSE = 		"No response (yet)";
	public static final String SELF_ALARM_ACTIVE = 		"Alarm Active";
	public static final String SELF_ALARM_COMPLETE	= 	"Alarm Complete";
	
	private String text;
	private String assigner;
	private String assignmentDate;
	private String status;
	private String lat;
	private String lon;
	private String locationName;
	private String type;
	
	public Alarm(){}
	
	public Alarm(String text, String assigner, String assignmentDate,
			String status,String lat, String lon, String locationName, String type) {
		this.text = text;
		this.assigner = assigner;
		this.assignmentDate = assignmentDate;
		this.status = status;
		this.lat = lat;
		this.lon = lon;
		this.locationName = locationName;
		this.type = type;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getAssigner() {
		return assigner;
	}
	public void setAssigner(String assigner) {
		this.assigner = assigner;
	}
	public String getAssignmentDate() {
		return assignmentDate;
	}
	public void setAssignmentDate(String assignmentDate) {
		this.assignmentDate = assignmentDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}
	
	public String getLocationName(){
		return locationName;
	}
	
	public void setLocationName(String locationName){
		this.locationName = locationName;
	}
	
	public String getType(){
		return type;
	}
	
	public void setType(String type){
		this.type = type;
	}
	
}


