package com.almende.cape.entity.timeline;

import java.io.Serializable;

public class Slot implements Serializable {

	private static final long serialVersionUID = 3684365130735099172L;
	int id;
	long start,end;
	String value;
	
	Slot() {id=0;start=0;end=0;value=""; }
	Slot(int i,long s,long e,String v){id=i;start=s;end=e;value=v;}
	
	public long getStart(){return start;}
	public long getEnd(){return end;}
	public String getValue(){return value;}
	public int getIDX(){return id;}
		
	public void setStart(long start) {this.start=start;}
	public void setEnd(long end) {this.end=end;}
	public void setIDX(int i){id=i;}
	public void setValue(String s){value=s;}
}
