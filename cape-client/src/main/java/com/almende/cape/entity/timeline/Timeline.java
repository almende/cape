package com.almende.cape.entity.timeline;

import java.util.ArrayList;

public interface Timeline {

	public void insertSlot(Slot slot);
	public void insertSlot(long start_ms,long end_ms, String value);
	public ArrayList<Slot> getSlots(boolean includeEmpty,  long period_from,long period_to );
}
