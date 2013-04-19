package com.almende.cape.entity.timeline;

import java.util.HashMap;

public class Week {
	
	private int day;	// range 1monday - 7sunday
	private int hour;
	private int minute;
	private int second;
	// millisecond?

	public Week()
	{
		day = 1;
		hour = 0;
		minute = 0;
		second = 0;
	}
	public Week( HashMap<String,Object> blob )
	{
		day = (Integer)blob.get("day") ;
		hour = (Integer)blob.get("hour") ;
		minute = (Integer)blob.get("minute") ;
		second = (Integer)blob.get("second") ;
	}
	
	//for serializer?
	public int getDay(){return day;}
	public int getHour(){return hour;}
	public int getMinute(){return minute;}
	public int getSecond(){return second;}
	
	public void setDay(int wd){day=wd;}
	public void setHour(int hr){hour=hr;}
	public void setMinute(int mnt){minute=mnt;}
	public void setSecond(int scnd){second=scnd;}
	
	//debug
	String toJSON()
	{
		return "{\"day\":"+day+",\"hour\":"+hour+",\"minute\":"+minute+",\"second\":"+second+"}";		
	}
}
