package com.almende.cape.entity.timeline;

import java.util.HashMap;

public class Event {

	private long pos;
	private String value;
	
	//empty constructor just for deserialize
	Event(){ pos=0; value=null;}
	Event(long ps, String vl){	pos = ps;	value = vl;	}

	Event(HashMap<String,Object> blob)
	{
		Object o = blob.get("pos");
		if( o.getClass().getName().equals("java.lang.Integer") )
			pos = (Integer)o ; 
		else 
			pos = (Long)o ;
		value = (String)blob.get("value");
	}
	
	//for serializer?
	public long getPos(){return pos;}
	public String getValue(){return value;}
	public void setPos(long ps){pos=ps;}
	public void setValue(String vl){value=vl;}
}
