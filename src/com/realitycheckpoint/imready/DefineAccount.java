package com.realitycheckpoint.imready;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DefineAccount extends Activity {
	
	public static final int NEW_ACCOUNT      = 0;
	public static final int EXISTING_ACCOUNT = 1;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.define_account);

        final Button newAccount      = (Button)findViewById(R.id.define_account_new_button);
        final Button existingAccount = (Button)findViewById(R.id.define_account_existing_button);
        final EditText userName = (EditText)findViewById(R.id.define_account_username);
        final EditText nickName = (EditText)findViewById(R.id.define_account_nickname);

        newAccount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	createAccount(NEW_ACCOUNT, userName.getText().toString(), nickName.getText().toString());
            }
        });
        
        existingAccount.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	createAccount(EXISTING_ACCOUNT,  userName.getText().toString(), nickName.getText().toString());
            }
        });
	}
	
	private void createAccount(int accountType, String username, String nickname){
		if (accountType == NEW_ACCOUNT) {
			final AndroidHttpClient http = AndroidHttpClient.newInstance("Android ImReady 0.1");
			try {
				URI uri = new URI("http://www.monkeysplayingpingpong.co.uk:54321/participants");
		    	HttpPost postRequest = new HttpPost(uri);
		    	BasicHttpParams params = new BasicHttpParams();
		    	params.setParameter("name", nickname);
		    	params.setParameter("username", username);
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
		    	    protected void onPostExecute(String username, String nickname) {
		    	    	if (username != null && nickname != null) {
			        		Toast.makeText(DefineAccount.this, "Account created <" + username + "> <" + nickname + ">", Toast.LENGTH_LONG).show();
			        		
			        		SharedPreferences.Editor preferences = getSharedPreferences(Main.PREFERENCES_NAME, MODE_PRIVATE).edit();
			                preferences.putBoolean("accountDefined", true);
			                preferences.putString("accountUserName", username);
			                preferences.putString("accountNickName", nickname);
			        		preferences.commit();
			                
			        		finish();

		    	    	} else {
		    	    		Toast.makeText(DefineAccount.this, "Failed: " +error, Toast.LENGTH_LONG).show();
		    	    	}
		    	    }
		    	}.execute(postRequest);
			} catch (URISyntaxException e) {
	    		Toast.makeText(DefineAccount.this, "Failed: " +e, Toast.LENGTH_LONG).show();
	     	}

		} else if (accountType == EXISTING_ACCOUNT) {
			final AndroidHttpClient http = AndroidHttpClient.newInstance("Android ImReady 0.1");
			try {
				URI uri = new URI("http://www.monkeysplayingpingpong.co.uk:54321/participant/" + username);
				HttpGet getRequest = new HttpGet(uri);
		    	new AsyncTask<HttpGet, Void, String>() {
		    		private Throwable error = null;
		    	    protected String doInBackground(HttpGet... request) {
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
			        		Toast.makeText(DefineAccount.this, "Your meeting '"+id+"' was created with id "+id, Toast.LENGTH_SHORT).show();
		    	    	} else {
		    	    		Toast.makeText(DefineAccount.this, "Failed: " +error, Toast.LENGTH_SHORT).show();
		    	    	}
		    	    }
		    	}.execute(getRequest);
			} catch (URISyntaxException e) {
	    		Toast.makeText(DefineAccount.this, "Failed: " +e, Toast.LENGTH_SHORT).show();
	     	}			
		}
	}
}
