package com.almende.cape.android;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.almende.cape.agent.LocationAgent;
import com.almende.eve.agent.AgentFactory;

public class LocationSimulation {
	private static String AGENT_ID = "location";
	private Handler mHandler = new Handler(Looper.getMainLooper());
	
	
	private AgentFactory factory = null;
	private LocationAgent agent = null;

	public void start(String username, String password, Activity context) {
		try {
			if (factory == null) {
				factory = AgentFactory.getInstance();
				if (factory == null) {
					throw new Exception("CAPE is not yet instantiated");
				}
			}
			
			if (agent != null) {
				throw new Exception ("LocationAgent already created");
			}
			if (factory.hasAgent(AGENT_ID)){
				agent = (LocationAgent) factory.getAgent(AGENT_ID);
			} else {
				agent = (LocationAgent) factory.createAgent(LocationAgent.class, AGENT_ID);	
			}
			agent.setAccount(username, password, "location");
			agent.connect();
			//agent.registerBuilding(); // TODO: implement registration to building
			agent.setAndroidContext(context);
			mHandler.post(new Runnable() {
		          public void run() {
		        	  try {
						agent.startSensor();
					} catch (Exception e) {
						e.printStackTrace();
					}
		          }
		       });
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop () {
		try {
			if (agent != null) {
				agent.stopSimulation();
				agent.stopSensor();
				//agent.unregisterBuilding(); // TODO: implement registration to building
				agent.disconnect();
				agent = null;
				factory.deleteAgent(AGENT_ID);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void useActualLocation() {
		try {
			agent.startSensor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void moveAway() {
		agent.startSimulation();
		
	}

}
