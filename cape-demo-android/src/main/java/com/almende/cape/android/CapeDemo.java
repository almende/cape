package com.almende.cape.android;

import java.util.logging.Logger;

import com.almende.cape.CapeClient;
import com.almende.cape.handler.NotificationHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CapeDemo extends Activity {
	private EditText txtUsername;
	private EditText txtPassword;
	private Button btnConnect;
	private Button btnDisconnect;
	private Button btnGetContacts;
	private TextView lblInfo;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cape_demo);

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

        btnGetContacts = (Button) findViewById(R.id.getContacts);
        btnGetContacts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new GetContactsTask().execute();
			}
        });
        
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
					}
				});
    			
    			logger.info("connected");
    			
    			// start location simulation
    			//locationSimulation.start(username, password);
    			
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
				txtUsername.setVisibility(View.INVISIBLE);
				txtPassword.setVisibility(View.INVISIBLE);
				btnConnect.setVisibility(View.INVISIBLE);
				btnDisconnect.setVisibility(View.VISIBLE);
				btnGetContacts.setVisibility(View.VISIBLE);	
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
    			//locationSimulation.stop();
    			
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
				txtUsername.setVisibility(View.VISIBLE);
				txtPassword.setVisibility(View.VISIBLE);
				btnConnect.setVisibility(View.VISIBLE);
				btnDisconnect.setVisibility(View.INVISIBLE);
				btnGetContacts.setVisibility(View.INVISIBLE);
    		}
    	}
    }

    class GetContactsTask extends AsyncTask<Void, String, String> {
		@Override
		protected String doInBackground(Void... params) {
			try {
				logger.info("getting contacts...");

    			ArrayNode contacts = cape.getContacts(null);
    			logger.info("contacts retrieved: " + contacts);
    			
    			return "contacts retrieved";
    		} catch (Exception e) {
    			e.printStackTrace();
    			return "failed to retrieve contacts";
    		}
		}

		@Override
    	protected void onPreExecute() {
			btnGetContacts.setEnabled(false);
    	}
		
		@Override
    	protected void onPostExecute(String state) {
			lblInfo.setText(state);
			btnGetContacts.setEnabled(true);
    	}
    }
    
    private CapeClient cape = new CapeClient();
    //private LocationSimulation locationSimulation = new LocationSimulation();
    
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());;
}
