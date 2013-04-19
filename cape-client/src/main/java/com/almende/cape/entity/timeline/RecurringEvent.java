package com.almende.cape.entity.timeline;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class RecurringEvent {

	Week from,till;
	String value;
	//String tz;	// how willl this work?
	

	//for serializer?
	public Week getFrom(){return from;}
	public Week getTill(){return till;}
	public String getValue(){return value;}
	
	public RecurringEvent()
	{
		from = new Week();
		till = new Week();
		value = null;
	}
	public RecurringEvent(HashMap<String,Object> blob)
	{
		this.from = new Week( (HashMap<String,Object>)blob.get("from") );
		this.till = new Week( (HashMap<String,Object>)blob.get("till") );
		this.value = (String)blob.get("value");
	}
	
	
	public RecurringEvent(int wd, int h,int m,int s,  int wd2, int h2,int m2,int s2, String v)
	{
		from = new Week();
		from.setDay((( wd +6)%7)+1);
		from.setHour(h %24);
		from.setMinute(m %60);
		from.setSecond(s %60);
		
		till = new Week();
		till.setDay((( wd2 +6)%7)+1);
		till.setHour(h2 %24);
		till.setMinute(m2 %60);
		till.setSecond(s2 %60);

		value = v;
	}
	
	ArrayList<Slot> calculateSlots(long period_from,long period_to )
	{
		// calculate tm struct for period_from
		DateTime joda_from = new DateTime(period_from);
		DateTime joda_till = new DateTime(period_to);
// System.out.println( "period["+ period_from +" "+ period_to+"]" );
					
		//1) calculate a recurring event start in this week 
		// TODO: add timezones offset
		DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
		DateTime start = formatter.parseDateTime( String.format("%02d/%02d/%04d %02d:%02d:%02d", 
				joda_from.getDayOfMonth(), joda_from.getMonthOfYear(), joda_from.getYear(), from.getHour(),from.getMinute(),from.getSecond() ) );
		start = start.dayOfWeek().setCopy(from.getDay());
		DateTime end = formatter.parseDateTime( String.format("%02d/%02d/%04d %02d:%02d:%02d", 
				joda_from.getDayOfMonth(), joda_from.getMonthOfYear(), joda_from.getYear(), till.getHour(),till.getMinute(),till.getSecond()) );
		end = end.dayOfWeek().setCopy(till.getDay());
		if( !start.isBefore(end) )end = end.dayOfWeek().addToCopy(7); //avoid negative period
		
		//silly startup problem
		start = start.dayOfWeek().addToCopy(-7);
		end = end.dayOfWeek().addToCopy(-7);
		while( joda_from.isAfter(end) )
		{
			start = start.dayOfWeek().addToCopy(7);
			end = end.dayOfWeek().addToCopy(7);
		}
		
		//output slots
		ArrayList<Slot> ret = new ArrayList<Slot>();
		while( start.isBefore(joda_till) )
		{
			Slot s = new Slot( 0, start.getMillis(),end.getMillis(), this.value );
			ret.add(s);
			
			start = start.dayOfWeek().addToCopy(7);
			end = end.dayOfWeek().addToCopy(7);
		}
		return ret;
	}
	
	//debug
	String toJSON()
	{
		return "{" + from.toJSON() + till.toJSON() + value + "}";		
	}
}
