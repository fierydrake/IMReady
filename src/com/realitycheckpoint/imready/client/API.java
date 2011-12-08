package com.realitycheckpoint.imready.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.http.AndroidHttpClient;
import android.os.Handler;

import com.realitycheckpoint.imready.IMReady;

public class API {
	public abstract static class Action<T> implements Runnable {
		final Handler handler = new Handler();
		public void run() {
			try { 
				final T result = action();
				handler.post(new Runnable() {
					public void run() { success(result); }
				});
			} catch (final APICallFailedException e) {
				e.printStackTrace();
				handler.post(new Runnable() {
					public void run() { failure(e); }
				});
			}
		}
		public abstract T action() throws APICallFailedException;
		public abstract void success(T result);
		public abstract void failure(APICallFailedException e);
	}
	public static void performInBackground(Action<?> action) {
		new Thread(action).start();
	}
	
	private static final String SERVER_URI = "http://www.monkeysplayingpingpong.co.uk:54321/"; 
	
	// GET
	public static void user(String id) throws APICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("user/" + id);
	    	HttpGet getRequest = new HttpGet(uri);
	    	HttpResponse response = http.execute(getRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 404: throw new APICallFailedException("User id '" + id + "' not found");
	    	case 500: throw new APICallFailedException("Internal error on server");
	    	default: throw new APICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new APICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new APICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new APICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}				
	}
	
	public static Meeting meeting(int id) throws APICallFailedException {
	    final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
	    try {
	        URI uri = new URI(SERVER_URI).resolve("meeting/" + id);
	        HttpGet getRequest = new HttpGet(uri);
	        HttpResponse response = http.execute(getRequest);

	        int status = response.getStatusLine().getStatusCode();
	        switch (status) {
	        case 200: // OK
	            String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	            try {
	                JSONObject meetingJSON = (JSONObject)new JSONTokener(body).nextValue();
	                ArrayList<Participant> participants = new ArrayList<Participant>();
	                JSONArray participantsJSON = meetingJSON.getJSONArray("participants");
	                for (int i=0; i<participantsJSON.length(); i++) {
	                    JSONObject participantJSON = participantsJSON.getJSONObject(i);
	                    participants.add(
	                        new Participant(
	                            new User(participantJSON.getString("id"), participantJSON.getString("defaultNickname")),
	                            participantJSON.getString("state"),
	                            participantJSON.getString("notified")
	                        )
	                    );
	                }
	                return new Meeting(meetingJSON.getInt("id"), meetingJSON.getString("name"), participants);
	            } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                throw new APICallFailedException("Server response invalid: " + e, e);
	            } catch (JSONException e) {
	                e.printStackTrace();
	                throw new APICallFailedException("Server response invalid: " + e, e);
	            }
	        case 404: throw new APICallFailedException("Meeting with id '" + id + "' not found");
	        case 500: throw new APICallFailedException("Internal error on server");
	        default: throw new APICallFailedException("Server returned unknown error: " + status);
	        }
	    } catch (URISyntaxException e) {
	        throw new APICallFailedException("[Internal] Server URI invalid", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new APICallFailedException("[Internal] Unsupported character encoding for form values", e);
	    } catch (IOException e) {
	        throw new APICallFailedException("Server returned an invalid response", e);
	    } finally {
	        http.close();
	    }
	}

//	public List<Meeting> meetings() throws APICallFailedException {
//		return null; 
//	}
//	
//	public List<Participant> meetingParticipants(int meetingId) throws APICallFailedException { 
//		return null; 
//	}
	
	// POST
	public static void createUser(String id, String defaultNickname) throws APICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("users");
	    	HttpPost postRequest = new HttpPost(uri);
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("id", id));
			nameValuePairs.add(new BasicNameValuePair("defaultNickname", defaultNickname));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(postRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	String msg = response.getEntity().toString(); // FIXME
	    	switch (status) {
	    	case 200: return; // OK
	    	case 400: throw new APICallFailedException(msg + " '" + id + "'");
	    	case 500: throw new APICallFailedException("Internal error on server");
	    	default: throw new APICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new APICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new APICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new APICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}		
	}
	
	public static int createMeeting(String creatorId, String name) throws APICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
    	String body = null;
    	String id = null;
		try {
			URI uri = new URI(SERVER_URI).resolve("meetings");
	    	HttpPost postRequest = new HttpPost(uri);
	    	
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("creator", creatorId));
			nameValuePairs.add(new BasicNameValuePair("name", name));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(postRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: // OK
		    	body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
		    	id = ((JSONObject)new JSONTokener(body).nextValue()).getString("id");
		    	return Integer.parseInt(id);
	    	case 404: throw new APICallFailedException("User id not found '" + id + "'");
	    	case 500: throw new APICallFailedException("Internal error on server");
	    	default: throw new APICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new APICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new APICallFailedException("[Internal] Unsupported character encoding for form values", e);
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
