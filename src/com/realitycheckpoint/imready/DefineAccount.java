package com.realitycheckpoint.imready;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;

import android.app.Activity;
import android.content.Intent;
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
	
	public static final int ACTIVITY_GET_ACCOUNT = 0;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if( getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getBoolean("accountDefined", false) ){
        	// skip to next activity
        	startActivityForResult( new Intent(DefineAccount.this, CreateMeeting.class), ACTIVITY_GET_ACCOUNT);
        } else {
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
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_GET_ACCOUNT){
    		finish();
    	}
    }

	private void createAccount(int accountType, String username, String nickname){
		if (accountType == NEW_ACCOUNT) {

			SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
            preferences.putString("FaccountUserName", username);
            preferences.putString("accountNickName", nickname);
            
			final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
			try {
				URI uri = new URI("http://www.monkeysplayingpingpong.co.uk:54321/participants");
		    	HttpPost postRequest = new HttpPost(uri);

		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("name", nickname));
		        nameValuePairs.add(new BasicNameValuePair("username", username));
		        try {
		        	postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		        } catch (UnsupportedEncodingException e){
		        	Toast.makeText(DefineAccount.this, "Hmm - not sure I like your account names...\n" + e, Toast.LENGTH_LONG).show();
		        }

		    	BasicHttpParams params = new BasicHttpParams();
		    	params.setParameter("name", nickname);
		    	params.setParameter("username", username);
		    	postRequest.setParams(params);
		    	new AsyncTask<HttpPost, Void, Boolean>() {
		    		private Throwable error = null;
		    	    protected Boolean doInBackground(HttpPost... request) {
		    	    	try {
			    	    	HttpResponse response = http.execute(request[0]);

			    	    	if(response.getStatusLine().getStatusCode() == 200) {
				    	    	return true;
			    	    	} else {
			    	    		return false;
				            }
			    	   	} catch (IOException e) {
			        		error = e;
			    	    }
			    	   	return false;
		    	    }
		    	    protected void onPostExecute(Boolean success) {
		    	    	if (success) {
			        		SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
			                preferences.putBoolean("accountDefined", true);
			                preferences.commit();
			                
			        		startActivityForResult( new Intent(DefineAccount.this, CreateMeeting.class), ACTIVITY_GET_ACCOUNT);
		    	    	} else {
		    	    		Toast.makeText(DefineAccount.this, "Failed: " +error, Toast.LENGTH_LONG).show();
		    	    	}
		    	    }
		    	}.execute(postRequest);
			} catch (URISyntaxException e) {
	    		Toast.makeText(DefineAccount.this, "Failed: " +e, Toast.LENGTH_LONG).show();
	     	}

		} else if (accountType == EXISTING_ACCOUNT) {
			
			SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
            preferences.putString("accountUserName", username);
            preferences.putString("accountNickName", nickname);

            final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
			try {
				URI uri = new URI("http://www.monkeysplayingpingpong.co.uk:54321/participant/" + username);
				HttpGet getRequest = new HttpGet(uri);
		    	new AsyncTask<HttpGet, Void, Boolean>() {
		    		private Throwable error = null;
		    	    protected Boolean doInBackground(HttpGet... request) {
		    	    	try {
			    	    	HttpResponse response = http.execute(request[0]);
			    
			    	    	if(response.getStatusLine().getStatusCode() == 200) {
				    	    	return true;
			    	    	} else {
			    	    		return false;
				            }
			    	   	} catch (IOException e) {
			        		error = e;
			    	    }
			    	   	return null;
		    	    }
		    	    protected void onPostExecute(Boolean success) {
		    	    	if (success) {
		    	    		SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
			                preferences.putBoolean("accountDefined", true);
			                preferences.commit();
			                
			        		startActivityForResult( new Intent(DefineAccount.this, CreateMeeting.class), ACTIVITY_GET_ACCOUNT);
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
