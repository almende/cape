package com.almende.cape.entity.timeline;

import java.util.ArrayList;
import java.util.HashMap;

import org.joda.time.DateTime;

import com.almende.eve.state.State;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TimelineWeekly {

	String ownerID;
	String type;
	ArrayList<RecurringEvent> eventList = null;
	
	public TimelineWeekly(String noPlanningValue)
	{
		eventList = new ArrayList<RecurringEvent>();
	}
	
	
	//jos persistance
	public boolean save_in_agentStore(State ctx)
	{
		try{
			ObjectMapper om = new ObjectMapper();
			String blob = om.writeValueAsString( this.eventList );
			ctx.put( "eventsWeekly", blob );
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("failed to serialize");
			return false;
		}
/*		
		//flexjson.JSONSerializer ser = new flexjson.JSONSerializer();
		//String json = ser.include("*").serialize(this.eventList); 
		//ctx.put( "eventsWeekly", json );
*/
		return true;
	}
	public static TimelineWeekly load_from_agentStore(State ctx)
	{
		TimelineWeekly  tlr = new TimelineWeekly(null);
		
		String blob = ctx.get("eventsWeekly", String.class);
		if( blob == null )return tlr;
		
		// flexjson.JSONDeserializer<java.util.ArrayList<cEvent_weeklyRecurring>> deser = new flexjson.JSONDeserializer<java.util.ArrayList<cEvent_weeklyRecurring>>();
		// tlr.eventList = deser.deserialize(json);

		try{
			// object mapping not working.. fix it in code. Tymon
			ObjectMapper om = new ObjectMapper();
			ArrayList al = om.readValue(blob, tlr.eventList.getClass() );
			tlr.eventList = new ArrayList<RecurringEvent>();
			for(int i=0;i<al.size();i++)
			{
				RecurringEvent we = new RecurringEvent( (HashMap<String,Object>)al.get(i) );
				tlr.eventList.add( we );
			}
			
		}
		catch( java.io.IOException ioe ){}
		
		return tlr;
	}
	
	//TODO: overlap function
	//TODO: add milliseconds?
	public void insertSlot(Slot s){	this.insertSlot( s.getStart(), s.getEnd(), s.getValue() );	}
	public void insertSlot(long start_ms,long end_ms, String value)
	{
		DateTime joda_from = new DateTime(start_ms);
		DateTime joda_till = new DateTime(end_ms);
		DateTime weekstart = new DateTime( start_ms ); 
		weekstart = weekstart.dayOfWeek().setCopy(1);
		
		//convert event to timeline
		TimelineOnce tl = new TimelineOnce(null);
		for(int i=eventList.size()-1;i>=0;i-- )
		{
			RecurringEvent cwr = eventList.get(i);
			DateTime start = new DateTime( weekstart );
			start = start.hourOfDay().setCopy( cwr.from.getHour() );
			start = start.minuteOfHour().setCopy( cwr.from.getMinute() );
			start = start.secondOfMinute().setCopy( cwr.from.getSecond() );
			start = start.dayOfWeek().setCopy( cwr.from.getDay() );
			
			DateTime end = new DateTime( weekstart );
			end = end.hourOfDay().setCopy( cwr.till.getHour() );
			end = end.minuteOfHour().setCopy( cwr.till.getMinute() );
			end = end.secondOfMinute().setCopy( cwr.till.getSecond() );
			end = end.dayOfWeek().setCopy( cwr.till.getDay() );
			if( start.isAfter(end) )end = end.dayOfWeek().addToCopy( 7 );
			
			tl.insertSlot( start.getMillis(), end.getMillis(), cwr.value );
		}
		
		//add our new one
		DateTime start = new DateTime( weekstart );
		start = start.hourOfDay().setCopy( joda_from.getHourOfDay() );
		start = start.minuteOfHour().setCopy( joda_from.getMinuteOfHour() );
		start = start.secondOfMinute().setCopy( joda_from.getSecondOfMinute() );
		start = start.dayOfWeek().setCopy( joda_from.getDayOfWeek() );
		
		DateTime end = new DateTime( weekstart );
		end = end.hourOfDay().setCopy( joda_till.getHourOfDay() );
		end = end.minuteOfHour().setCopy( joda_till.getMinuteOfHour() );
		end = end.secondOfMinute().setCopy( joda_till.getSecondOfMinute() );
		end = end.dayOfWeek().setCopy( joda_till.getDayOfWeek() );
		if( start.isAfter(end) )end = end.dayOfWeek().addToCopy( 7 );
		
		//whole week: empty planboard and put single slot
		if( end_ms > start_ms && start.getMillis() == end.getMillis())
		{
			tl = new TimelineOnce(null);
			start = new DateTime().withDayOfWeek(1).withTime(0, 0, 0, 0);
			end   = new DateTime(start).plusWeeks(1);
			
			System.out.println("fill entire week "+start.getMillis() +"/"+ end.getMillis() );
		}
		
		tl.insertSlot( start.getMillis(), end.getMillis(), value );
		
		// convert timeline back to events..
		ArrayList<Slot> slots = tl.getSlots(false, 0,0);
		eventList.clear();
		for(int i=0;i<slots.size();i++)
		{
			Slot slot = slots.get(i);
			joda_from = new DateTime(slot.getStart() );
			joda_till = new DateTime(slot.getEnd() );
			RecurringEvent cwr = new RecurringEvent(
					joda_from.getDayOfWeek(),joda_from.getHourOfDay(),joda_from.getMinuteOfHour(),joda_from.getSecondOfMinute(),
					joda_till.getDayOfWeek(),joda_till.getHourOfDay(),joda_till.getMinuteOfHour(),joda_till.getSecondOfMinute(),
					slot.getValue() );
			
//System.out.println("adding "+ cwr.toJSON() );
			
			eventList.add( cwr );
		}
		
	}

	/*
	public static void test()
	{
		TimelineRecurring tlr = new TimelineRecurring(null);
	
		cEvent_weeklyRecurring ce = null;
		for(int i=1;i<=7;i++)
		{
			ce = new cEvent_weeklyRecurring( i, 12,0,0,  i, 15,0,0, "hoi"+i );
			tlr.eventList.add(ce);
		}
			
		tlr.insertSlot( System.currentTimeMillis(),  System.currentTimeMillis()+3600*1000L, "hoi3" );
		
	}
	*/

	public ArrayList<Slot> getSlots(boolean includeEmpty,  long period_from,long period_to )
	{
		java.util.ArrayList<Slot> ret = null;
		
		for(int i=0;i< eventList.size();i++)
		{
			RecurringEvent cwr = eventList.get(i);
			
			if(ret == null)
			{
				ret = cwr.calculateSlots( period_from, period_to );
				continue;
			}
			
			//unneeded sorted insert complexity	--> ret.addAll(subRet)
			java.util.ArrayList<Slot> subRet = cwr.calculateSlots( period_from, period_to ); 
			for(Slot s: subRet)
			{
				s.setIDX(i);
				
				if( ret.size()==0 || s.getStart() <= ret.get(0).getStart() )
				{
					ret.add(0,s);
				}
				else
				if( s.getStart() >= ret.get( ret.size()-1).getStart() )
				{
					ret.add( s);					
				}
				else
				{
					//broken binary search
					int limit=10;
					int a=0,b=ret.size()-1;
					while(a+1<b && limit>0)
					{
						if( s.getStart() > ret.get( (a+b)/2 ).getStart() )
							a = (a+b)/2;
						else
							b = (a+b)/2;

						limit--;
					}
					ret.add( b, s);
				}
				
			}
			
		}
		
		if(ret == null)return new java.util.ArrayList<Slot>();
		return ret;
	}

	public TimelineOnce getAsTimeline(long startTime, long endTime)
	{
		TimelineOnce ret = new TimelineOnce(null);
		ArrayList<Slot> slots = this.getSlots(false,  startTime,endTime );
		for(int i=0;i<slots.size();i++)
		{
			ret.insertSlot( slots.get(i) );
		}
		return ret;
	}
}
