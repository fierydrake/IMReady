package com.realitycheckpoint.imready;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity {
	
	public static final String PREFERENCES_NAME = "IMReadyPrefs";
	private static final int ACTIVITY_GET_ACCOUNT = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	boolean accountDefined = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getBoolean("accountDefined", false);
        if(!accountDefined){
        	// Launch activity for account def.
        	startActivityForResult( new Intent(Main.this, DefineAccount.class), ACTIVITY_GET_ACCOUNT);
        }
        
        setContentView(R.layout.main);
        
        final Button createMeeting = (Button)findViewById(R.id.create_meeting_button);
        final EditText meetingName = (EditText)findViewById(R.id.meeting_name);
        
        String nickName = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString("accountNickName", "");
        if (!nickName.equalsIgnoreCase("")) {
            TextView welcomeLine = (TextView)findViewById(R.id.main_nickname);
        	welcomeLine.setText("Hello " + nickName);
        }

        createMeeting.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	createMeeting(meetingName.getText().toString());
            }
        });
        
        meetingName.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent event) {
            	// If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                	createMeeting(meetingName.getText().toString());
                	return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_GET_ACCOUNT){
    		if(resultCode == 0){
    			String nickName = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString("accountNickName", "");
    	        if (!nickName.equalsIgnoreCase("")) {
    	            TextView welcomeLine = (TextView)findViewById(R.id.main_nickname);
    	        	welcomeLine.setText("Hello " + nickName);
    	        }
    		}
    	}
    }

    private void createMeeting(final String name) {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance("Android ImReady 0.1");
		try {
			URI uri = new URI("http://www.monkeysplayingpingpong.co.uk:54321/meetings");
	    	HttpPost postRequest = new HttpPost(uri);
	    	BasicHttpParams params = new BasicHttpParams();
	    	params.setParameter("name", name);
	    	postRequest.setParams(params);
	    	new AsyncTask<HttpPost, Void, String>() {
	    		private Throwable error = null;
	    	    protected String doInBackground(HttpPost... request) {
	    	    	try {
		    	    	HttpResponse response = http.execute(request[0]);
		    	    	String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
		    	    	String id = ((JSONObject)new JSONTokener(body).nextValue()).getString("id");
		    	    	return id;
	    	    	} catch (JSONException e) {
	    	    		error = e;
		    	   	} catch (IOException e) {
		        		error = e;
		    	    }
		    	   	return null;
	    	    }
	    	    protected void onPostExecute(String id) {
	    	    	if (id != null) {
		        		Toast.makeText(Main.this, "Your meeting '"+name+"' was created with id "+id, Toast.LENGTH_SHORT).show();
	    	    	} else {
	    	    		Toast.makeText(Main.this, "Failed: " +error, Toast.LENGTH_SHORT).show();
	    	    	}
	    	    }
	    	}.execute(postRequest);
		} catch (URISyntaxException e) {
    		Toast.makeText(Main.this, "Failed: " +e, Toast.LENGTH_SHORT).show();
     	}
		
		SharedPreferences.Editor preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit();
		preferences.remove("accountDefined");
        preferences.remove("accountUserName");
        preferences.remove("accountNickName");
		preferences.commit();
    }
}