package com.almende.cape.agent.intf;

import java.util.List;

import com.almende.cape.entity.timeline.Slot;
import com.almende.eve.agent.AgentInterface;
import com.almende.eve.rpc.annotation.Name;

public interface CapeStateAgentIntf extends AgentInterface {

	public boolean setSlot(
			@Name("start_millis") long startTime,
			@Name("end_millis") long endTime,
			@Name("description") String desc,
			@Name("occurence") String occurence);
	
	public Slot getCurrentSlot(String occurence);
	public Slot getCurrentSlotCombined();
	
	public List<Slot> getSlots(
			@Name("start_millis") long startTime,
			@Name("end_millis") long endTime,
			@Name("occurence") String occurence );
	
	
	public List<Slot> getSlotsCombined(
			@Name("start_millis")long startTime,
			@Name("end_millis")long endTime);
}
