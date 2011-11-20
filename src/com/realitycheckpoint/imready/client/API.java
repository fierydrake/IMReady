package com.realitycheckpoint.imready.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.http.AndroidHttpClient;

import com.realitycheckpoint.imready.IMReady;

public class API {
	private static final String SERVER_URI = "http://www.monkeysplayingpingpong.co.uk:54321/"; 
	private User me;
	
	public API(User me) {
		this.me = me;
	}
	
	// GET
	public List<Meeting> meetings() throws APICallFailedException {
		return null; 
	}
	
	public List<Participant> meetingParticipants(int meetingId) throws APICallFailedException { 
		return null; 
	}
	
	// POST
	public void createUser(String id, String defaultNickname) throws APICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("users");
	    	HttpPost postRequest = new HttpPost(uri);
	    	BasicHttpParams params = new BasicHttpParams();
	    	params.setParameter("id", id);
	    	params.setParameter("defaultNickname", defaultNickname);
	    	postRequest.setParams(params);
	    	HttpResponse response = http.execute(postRequest);

	    	if (response.getStatusLine().getStatusCode() != 200) {
	    		throw new APICallFailedException("User id '" + id + "' taken");
	    	}
		} catch (URISyntaxException e) {
			throw new APICallFailedException("[Internal] Server URI invalid", e);
	   	} catch (IOException e) {
	   		throw new APICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}		
	}
	
	public int createMeeting(String name) throws APICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
    	String body = null;
    	String id = null;
		try {
			URI uri = new URI(SERVER_URI).resolve("meetings");
	    	HttpPost postRequest = new HttpPost(uri);
	    	BasicHttpParams params = new BasicHttpParams();
	    	params.setParameter("name", name);
	    	postRequest.setParams(params);
	    	HttpResponse response = http.execute(postRequest);
	    	
	    	body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	    	id = ((JSONObject)new JSONTokener(body).nextValue()).getString("id");
	    	
	    	return Integer.parseInt(id);
		} catch (URISyntaxException e) {
			throw new APICallFailedException("[Internal] Server URI invalid", e);
		} catch (NumberFormatException e) {
    		String bodyAsString = (body == null) ? ("null") : ("'" + body + "'");
    		throw new APICallFailedException("Server returned an invalid meeting id: '" + bodyAsString + "'" , e);
    	} catch (JSONException e) {
    		String idAsString = (id == null) ? ("null") : ("'" + id + "'");
    		throw new APICallFailedException("Server returned an invalid response: '" + idAsString + "'" , e);
	   	} catch (IOException e) {
	   		throw new APICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}
	}
	
	// PUT
	public void addMeetingParticipant(int meetingId, String userId) throws APICallFailedException {
	}
	
	public void ready(int meetingId) throws APICallFailedException {
	}
}
