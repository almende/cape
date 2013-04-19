package com.almende.cape.entity.timeline;

import java.util.ArrayList;
import java.util.HashMap;

import com.almende.eve.state.State;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TimelineOnce implements Timeline {


	String ownerID;
	String type;
	ArrayList<Event> eventList = null;
	
	//constructor for deserialize
	TimelineOnce()
	{
		eventList = new ArrayList<Event>();
		events_addState( 1, null );
	}
	public TimelineOnce(String initial)
	{
		eventList = new ArrayList<Event>();
		events_addState( 1, initial);
	}
	
	
	// getters for serializer..
	public String getOwnerID(){return ownerID;}
	public String getType(){return type;}
	public java.util.ArrayList<Event> getEventList(){return eventList;}
	
	
	// jos persistance
	public boolean save_in_agentStore(State ctx)
	{
		//flexjson.JSONSerializer ser = new flexjson.JSONSerializer();
		//String blob = ser.include("*").serialize(this.eventList); 
		//ctx.put( "events", blob );
		
		try{
			ObjectMapper om = new ObjectMapper();
			String blob = om.writeValueAsString( this.eventList );
			ctx.put( "events", blob );
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("failed to serialize");
			return false;
		}
		
		return true;
	}
	public static TimelineOnce load_from_agentStore(State ctx)
	{
		TimelineOnce tl = new TimelineOnce(null);
		
		String blob =(String)ctx.get("events");
		if( blob == null )return tl;
		
		// flexjson.JSONDeserializer<java.util.ArrayList<cEvent>> deser = new flexjson.JSONDeserializer<java.util.ArrayList<cEvent>>();
		// tl.eventList = deser.deserialize(blob);
			
		try{
			// object mapping not working.. fix it in code. Tymon
			ObjectMapper om = new ObjectMapper();
			ArrayList al = om.readValue(blob, tl.eventList.getClass() );
			tl.eventList = new ArrayList<Event>();
			for(int i=0;i<al.size();i++)
			{
				Event e = new Event( (HashMap)al.get(i) );
				tl.eventList.add(e);
			}
		}
		catch(java.io.IOException ioe)
		{
			System.err.println("failed to deserizalize");
			//return tl anyway
		}
		
		
		return tl;		
	}
	
	private int events_addState(long pos, String value)
	{
			int index = events_getIndex(pos);
			eventList.add(index, new Event(pos,value) );
			return index;
	}
	
	private int events_getIndex(long pos)
	{
		if( pos == 0 )
			pos = System.currentTimeMillis();
		
		int i;
		int index = eventList.size();
		for(i=0;i<index;i++)
		{
			if( pos < eventList.get(i).getPos() )
			{
				index = i;
				break;
			}
		}
		return index;
	}
	
	
	private int events_dupeState(long pos)
	{
		 int index = events_getIndex(pos);

		 //no need to dupe
		 if(index>=1 && index< eventList.size()+1 && eventList.get(index-1).getPos() == pos )
		 {
			 return index-1;
		 }
		 
		 eventList.add(index, new Event(pos,"") );	// NO_STATE
		 if( index > 0 )
		 {
			 	eventList.get(index).setValue( eventList.get(index-1).getValue() );
		 }
		 return index;
	}

	//remove markers BETWEEN? start and end + skip epoch marker
	private void events_clear(int start_index, int end_index)
	{
		// ArrayList.removeRange(int fromIndex, int toIndex)
		int delta = (end_index-start_index-1);
		for(int i=0;i<delta;i++)
		{
			eventList.remove(start_index+1);
		}
	}
	
	private void events_glue()
	{
		int usedGlue;
		do
		{
			usedGlue = 0;
			
			//remove same values
			for(int i=eventList.size()-1;i>=1;i--)
			{
				String a = eventList.get(i).getValue();
				String b = eventList.get(i-1).getValue();
				if( (a!=null && a.equals(b)) || (a==null&&b==null)  )	// NULL complications
				{
					eventList.remove(i);
					usedGlue++;
				}
			}
			//remove same time
			for(int i=eventList.size()-2;i>=0;i--)
			{
				if( eventList.get(i).getPos() == eventList.get(i+1).getPos() )
				{
					eventList.remove(i);
					usedGlue++;
				}
			}
			
		}while(usedGlue>0);
	}
	
	public void events_clip(long period_from, long period_to)
	{
		int start = events_dupeState( period_from );
	    int end = events_dupeState( period_to );
	    
	    //remove end part
		// ArrayList.removeRange(int fromIndex, int toIndex)
	    int delta = eventList.size()-(end+1);
	    for(int i= 0; i < delta ;i++ )
	    	eventList.remove(end+1);
	    
	    //remove begin part
	    events_clear( 0, start );

	//  System.out.println("clip ["+period_from+";"+period_to+"] got"+ start+" "+end );
	//  debug("removed..");
	    
	    events_glue();
	}
	
	
		/////////////////////////////

	
	//construct ( use null for empty planboard)
		
	public void insertSlot(Slot slot){	insertSlot( slot.start, slot.end, slot.value );	}
	public void insertSlot(long start_ms,long end_ms, String value)
	{
		//sanity
		if( end_ms <= start_ms )return;
		
		int end_index = events_dupeState(end_ms);
		int start_index = events_addState(start_ms,value);
		end_index++;
		events_clear( start_index,end_index);
		events_glue();
	}

	//TODO: add overlap functionPointer :(
	String calc(String a,String b){ if(a==null&&b==null)return ";"; if(a==null)return ";"+b; if(b==null)return a+";"; return a+";"+b; }
	public void combine(TimelineOnce src, String calcFunction )
	{
		String base=null,delta=null;
		int si=0; //counters instead of iterators..
		int di=0;
		while( di < this.eventList.size() && si < src.eventList.size() )
		{
			if( this.eventList.get(di).getPos() == src.eventList.get(si).getPos() )
			{
				delta = src.eventList.get(si).getValue();
				base  = this.eventList.get(di).getValue();
				this.eventList.get(di).setValue( calc(base,delta) );
				di++;
				si++;
			}
			else if( this.eventList.get(di).getPos()< src.eventList.get(si).getPos() )
			{
				base = this.eventList.get(di).getValue();
				this.eventList.get(di).setValue( calc(base,delta) );
				di++;
			}
			else if( src.eventList.get(si).getPos() < this.eventList.get(di).getPos() )
			{
				delta = src.eventList.get(si).getValue();
				int index = this.events_addState( src.eventList.get(si).getPos(), calc(base,delta) );
				// copy other items: this.eventList[index].extra = src.eventList[si].extra
				di++;
				si++;
			}
			else
				System.err.print("# weirdness");	// throw Exeception?
		}
		while( di < this.eventList.size() )
		{
			base = this.eventList.get(di).getValue();
			this.eventList.get(di).setValue( calc(base,delta) );
			di++;
		}
		while( si < src.eventList.size() )
		{
			delta = src.eventList.get(si).getValue();
			int index = this.events_addState( src.eventList.get(si).getPos(), calc(base,delta)  );
			// copy other items: this.eventList[index].extra = src.eventList[si].extra
			si++;
		}

		this.events_glue(); //not needed?
	}

	/*
	void debug(String title)
	{
		System.out.println(" ---+-------- "+ this.ownerID+"/"+this.type + "("+ eventList.size()+ ") "+title );
		for(int i=0;i<eventList.size();i++)
		{
			cEvent e = eventList.get(i);
			System.out.format("%2d | %8d %s \n", i, e.getPos(), e.getValue() ) ;
		}
	}
	*/
	
	// additional clipping
	public ArrayList<Slot> getSlots(boolean includeEmpty,  long period_from,long period_to )
	{
		ArrayList<Slot> slots = new ArrayList<Slot>();
		
		//empty planboard bailout
		if( includeEmpty && eventList.size() <= 1 )
		{
			slots.add( new Slot(0, period_from, period_to, eventList.get(0).getValue() ) );
			return slots;
		}
		
		int i = 0;
		
		// append zero space at start
		if( includeEmpty && eventList.get(0).getValue() == null && eventList.get(0).getPos() <= 1 )
		{
			if( eventList.get(i+1).getPos() > period_from )
			{
				slots.add( new Slot(0, period_from, eventList.get(i+1).getPos(), eventList.get(i).getValue() ) );
			}
			i++;
		}
		
		for(;i< eventList.size()-1;i++)
		{
			// return nonzero-spaces only
         if( !includeEmpty && eventList.get(i).getValue() == null )continue;
         
         if( eventList.get(i+1).getPos() <= period_from )continue;
         if( period_to > period_from && eventList.get(i  ).getPos() >= period_to   )break;

         slots.add( new Slot(i, eventList.get(i).getPos(), eventList.get(i+1).getPos(), eventList.get(i).getValue() ) );
		}
		
		return slots;
	}
	
	// this will fail if "before first slot" or "after last slot" in timeline (obvious)
	public Slot extractSingleSlot(long pos)
	{
		int end = events_getIndex( pos );
		if( end < 1 || end >= eventList.size() )return null;

		Event current = eventList.get(end-1);
		Event next = eventList.get(end);
		return new Slot(end-1, current.getPos(), next.getPos(), current.getValue() );
	}
}
