package com.almende.cape.android;


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
import com.almende.cape.handler.NotificationHandler;

public class CapeDemo extends Activity {
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cape_demo);
        
        ctx = this;
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
    			cape.onNotification(new NotificationHandler() {
					@Override
					public void onNotification(String message) {
						logger.info("Notification: " + message);
						lblInfo.setText(message);
						lblInfo.postInvalidate();
					}
				});
    			
    			logger.info("connected");
    			
    			// start location simulation
    			locationSimulation.start(username, password, ctx);
    			
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

    
    private CapeClient cape = new CapeClient();
    private LocationSimulation locationSimulation = new LocationSimulation();
    
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());;
}
