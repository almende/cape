package com.almende.cape.android;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.almende.cape.CapeClient;
import com.almende.cape.entity.Message;
import com.almende.cape.handler.MessageHandler;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.scheduler.RunnableSchedulerFactory;
import com.almende.eve.state.AndroidStateFactory;

public class CapeDemo extends Activity {
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	
	private EditText txtUsername;
	private EditText txtPassword;
	private Button btnConnect;
	private Button btnDisconnect;
	private Button btnUseActualLocation;
	private Button btnMoveAway;
	@SuppressWarnings("unused")
	private TextView lblLocation;
	private TextView lblInfo;
	Activity ctx = null;
    private CapeClient cape = null;
    private LocationSimulation locationSimulation = null;

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cape_demo);
        
        ctx = this;
        try {
        	AgentHost af = AgentHost.getInstance();
        	Map<String, Object> params = new HashMap<String,Object>();
        	params.put("AppContext", ctx);
        
        	af.setStateFactory(new AndroidStateFactory(params));
        	af.setSchedulerFactory(new RunnableSchedulerFactory(af, ".runnablescheduler"));
		
        	cape = new CapeClient(af);
        	locationSimulation = new LocationSimulation();
        } catch (Exception e){
        	logger.severe("Couldn't start cape/eve framework!");
        	e.printStackTrace();
        }
        txtUsername = (EditText) findViewById(R.id.username);
        txtPassword = (EditText) findViewById(R.id.password);
        btnConnect = (Button) findViewById(R.id.connect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final String username = txtUsername.getText().toString();
				final String password = txtPassword.getText().toString();
				new CapeConnect().execute(username, password);
			}
        });
        
        btnDisconnect = (Button) findViewById(R.id.disconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new CapeDisconnect().execute();
			}
        });

        btnUseActualLocation = (Button) findViewById(R.id.useActualLocation);
        btnUseActualLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				locationSimulation.useActualLocation();
			}
        });
        
        btnMoveAway = (Button) findViewById(R.id.moveAway);
        btnMoveAway.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				locationSimulation.moveAway();
			}
        });
        
        lblLocation = (TextView) findViewById(R.id.location);
        lblInfo = (TextView) findViewById(R.id.info);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_cape_demo, menu);
        return true;
    }
    
    class CapeConnect extends AsyncTask<String, String, String> {
    	@Override
    	protected String doInBackground(String... params) {
    		try {
    			String username = params[0];
    			String password = params[1];
    			
    			// login to cape
    			logger.info("connecting user " + username);
    			cape.login(username, password);
    			cape.onMessage(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						logger.info("Notification: " + message.getMessage());
						lblInfo.setText(message.getMessage());
						lblInfo.postInvalidate();
					}
				});
    			
    			logger.info("connected");
    			
    			// start location simulation
    			locationSimulation.start(username, password);
    			
    			return "connected";
    		} catch (Exception e) {
    			e.printStackTrace();
    			return "failed to connect";
    		}
    	}

		@Override
    	protected void onPreExecute() {
			btnConnect.setEnabled(false);
    	}
		
		@Override
    	protected void onPostExecute(String state) {
			lblInfo.setText(state);

			btnConnect.setEnabled(true);
    		if (state.equals("connected")) {
    			txtUsername.setEnabled(false);
    			txtPassword.setEnabled(false);
    			btnConnect.setEnabled(false);
    			btnDisconnect.setEnabled(true);
    			btnUseActualLocation.setEnabled(true);
    			btnMoveAway.setEnabled(true);
    		}
    	}
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("txtUsername", txtUsername.isEnabled());
        outState.putBoolean("txtPassword", txtPassword.isEnabled());
        outState.putBoolean("btnConnect", btnConnect.isEnabled());
        outState.putBoolean("btnDisconnect", btnDisconnect.isEnabled());
        outState.putBoolean("btnUseActualLocation", btnUseActualLocation.isEnabled());
        outState.putBoolean("btnMoveAway", btnMoveAway.isEnabled());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            txtUsername.setEnabled(savedInstanceState.getBoolean("txtUsername"));
            txtPassword.setEnabled(savedInstanceState.getBoolean("txtPassword"));
            btnConnect.setEnabled(savedInstanceState.getBoolean("btnConnect"));
            btnDisconnect.setEnabled(savedInstanceState.getBoolean("btnDisconnect"));
            btnUseActualLocation.setEnabled(savedInstanceState.getBoolean("btnUseActualLocation"));
            btnMoveAway.setEnabled(savedInstanceState.getBoolean("btnMoveAway"));
        }
    }
    
    class CapeDisconnect extends AsyncTask<Void, String, String> {
		@Override
		protected String doInBackground(Void... params) {
			try {
				logger.info("disconnecting...");

    			// logout from cape
    			cape.logout();
    			
    			logger.info("disconnected");
    			
    			// stop location simulation
    			locationSimulation.stop();
    			
    			return "disconnected";
    		} catch (Exception e) {
    			e.printStackTrace();
    			return "failed to disconnect";
    		}
		}

		@Override
    	protected void onPreExecute() {
			btnDisconnect.setEnabled(false);
    	}
		
		@Override
    	protected void onPostExecute(String state) {
			lblInfo.setText(state);
			
			btnDisconnect.setEnabled(true);
    		if (state.equals("disconnected")) {
				txtUsername.setEnabled(true);
    			txtPassword.setEnabled(true);
    			btnConnect.setEnabled(true);
    			btnDisconnect.setEnabled(false);
    			btnUseActualLocation.setEnabled(false);
    			btnMoveAway.setEnabled(false);
    		}
    	}
    }


}
