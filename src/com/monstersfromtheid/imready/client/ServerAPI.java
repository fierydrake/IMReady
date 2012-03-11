package com.monstersfromtheid.imready.client;

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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.net.http.AndroidHttpClient;
import android.os.Handler;

import com.monstersfromtheid.imready.IMReady;

public class ServerAPI {
	public abstract static class Action<T> implements Runnable {
		final Handler handler = new Handler();
		public void run() {
			try { 
				final T result = action();
				handler.post(new Runnable() {
					public void run() { success(result); }
				});
			} catch (final ServerAPICallFailedException e) {
				//e.printStackTrace();
				handler.post(new Runnable() {
					public void run() { failure(e); }
				});
			}
		}
		public abstract T action() throws ServerAPICallFailedException;
		public abstract void success(T result);
		public abstract void failure(ServerAPICallFailedException e);
	}
	public static void performInBackground(Action<?> action) {
		new Thread(action).start();
	}
	
	
	
	private String requestingUserId;
	
	public ServerAPI(String requestingUserId) {
		this.requestingUserId = requestingUserId;
	}
	public String getRequestingUserId() { return requestingUserId; }
	
	// GET

	/**
	 * Check to see if the userID exists in the server.
	 * 
	 * @param userId - ID of the user to retrieve
	 * @return
	 * @throws ServerAPICallFailedException
	 */
	public void user(String userId) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("user/" + userId);
	    	HttpGet getRequest = new HttpGet(uri);
	    	getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	HttpResponse response = http.execute(getRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 404: throw new ServerAPICallFailedException("User id '" + userId + "' not found");
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}				
	}

	/**
	 * Retrieve the details from the server of the meeting with id meetingId.
	 * 
	 * @param meetingId - ID of the meeting to retrieve
	 * @return - JSONObject of the meeting
	 * @throws ServerAPICallFailedException
	 */
	public Meeting meeting(int meetingId) throws ServerAPICallFailedException {
	    final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
	    try {
	        URI uri = new URI(IMReady.SERVER_URI).resolve("meeting/" + meetingId);
	        HttpGet getRequest = new HttpGet(uri);
	        getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	        HttpResponse response = http.execute(getRequest);

	        int status = response.getStatusLine().getStatusCode();
	        switch (status) {
	        case 200: // OK
	            String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	            try {
	            	JSONObject meetingJSON = new JSONObject(body);
	            	ArrayList<Participant> participants = new ArrayList<Participant>();
	    	    	JSONArray participantsJSON = meetingJSON.getJSONArray("participants");
	    	    	for (int i=0; i<participantsJSON.length(); i++) {
	    	    		JSONObject participantJSON = participantsJSON.getJSONObject(i);
	    	    		participants.add(
	    	    				new Participant(
	    	    						new User(participantJSON.getString("id"), participantJSON.getString("defaultNickname")),
	    	                            participantJSON.getInt("state"),
	    	                            participantJSON.getBoolean("notified")
	    	                        )
	    	                    );
	    	        }
	    	    	boolean decorated = false;
	    	    	try {
	    	    		decorated = meetingJSON.getBoolean("notified");
	    	    	} catch (JSONException e) {
	    	    		// Assume decorated is false
	    	    	}
	    	    	boolean newToUser = false;
	    	    	try {
	    	    		newToUser = meetingJSON.getBoolean("newToUser");
	    	    	} catch (JSONException e) {
	    	    		// Assume newToUser is false
	    	    	}
	    	    	boolean changedToUser = false;
	    	    	try {
	    	    		changedToUser = meetingJSON.getBoolean("changedToUser");
	    	    	} catch (JSONException e) {
	    	    		// Assume changedToUser is false
	    	    	}
	    	    	
	    	    	return new Meeting(meetingJSON.getInt("id"), 
	    	    			meetingJSON.getString("name"), 
	    	    			meetingJSON.getInt("state"), 
	    	    			participants, 
	    	    			decorated, 
	    	    			newToUser, 
	    	    			changedToUser);
	            } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            } catch (JSONException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            }
	        case 404: throw new ServerAPICallFailedException("Meeting with id '" + meetingId + "' not found");
	        case 500: throw new ServerAPICallFailedException("Internal error on server");
	        default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	        }
	    } catch (URISyntaxException e) {
	        throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	    } catch (IOException e) {
	        throw new ServerAPICallFailedException("Server returned an invalid response", e);
	    } finally {
	        http.close();
	    }
	}

	/**
	 * Return the meetings that user userID is a part of.
	 * 
	 * @param userId - ID of the user to retrieve the meetings of
	 * @return - JSONArray of the meetings for a given user
	 * @throws ServerAPICallFailedException
	 */
	public JSONArray userMeetings(String userId) throws ServerAPICallFailedException {
	    final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
	    try {
	        URI uri = new URI(IMReady.SERVER_URI).resolve("meetings/" + userId);
	        HttpGet getRequest = new HttpGet(uri);
	        getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	        HttpResponse response = http.execute(getRequest);

	        int status = response.getStatusLine().getStatusCode();
	        switch (status) {
	        case 200: // OK
	            String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	            try {
	    			return new JSONArray(body);
	            } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            } catch (JSONException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            }
	        case 404: throw new ServerAPICallFailedException("User with id '" + userId + "' not found");
	        case 500: throw new ServerAPICallFailedException("Internal error on server");
	        default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	        }
	    } catch (URISyntaxException e) {
	        throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
	    } catch (UnsupportedEncodingException e) {
	        throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	    } catch (IOException e) {
	        throw new ServerAPICallFailedException("Server returned an invalid response", e);
	    } finally {
	        http.close();
	    }
	}

	// POST

	/**
	 * Calls the server to create a user with the given userID and nickname.
	 * 
	 * @param userId - the userID of the user to create
	 * @param defaultNickname - the nickname to give a user
	 * @throws ServerAPICallFailedException
	 */
	public void createUser(String userId, String defaultNickname) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("users");
	    	HttpPost postRequest = new HttpPost(uri);
	    	postRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("id", userId));
			nameValuePairs.add(new BasicNameValuePair("defaultNickname", defaultNickname));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(postRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	String msg = response.getEntity().toString(); // FIXME - not sure why this comment is here.
	    	switch (status) {
	    	case 200: return; // OK
	    	case 400: throw new ServerAPICallFailedException(msg + " '" + userId + "'");
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}		
	}
	
	/**
	 * Calls the server to create a meeting with the given name.
	 * 
	 * @param creatorId - The userID of the user creating the meeting.  They will be put as the first participant.
	 * @param name - The name for the meeting
	 * @return - the ID of the meeting that was created
	 * @throws ServerAPICallFailedException
	 */
	public int createMeeting(String creatorId, String name) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
    	String body = null;
    	String id = null;
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("meetings");
	    	HttpPost postRequest = new HttpPost(uri);
	    	postRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	
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
	    	case 404: throw new ServerAPICallFailedException("User id not found '" + id + "'");
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
		} catch (NumberFormatException e) {
    		String bodyAsString = (body == null) ? ("null") : ("'" + body + "'");
    		throw new ServerAPICallFailedException("Server returned an invalid meeting id: '" + bodyAsString + "'" , e);
    	} catch (JSONException e) {
    		String idAsString = (id == null) ? ("null") : ("'" + id + "'");
    		throw new ServerAPICallFailedException("Server returned an invalid response: '" + idAsString + "'" , e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}
	}
	
	// POST

	/**
	 * Calls the server to add a user to the given meeting.
	 * 
	 * @param meetingId - The ID of the meeting to add a user to
	 * @param userId - The ID of the user to add
	 * @throws ServerAPICallFailedException
	 */
	public void addMeetingParticipant(int meetingId, String userId) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("meeting/" + meetingId + "/participants");
	    	HttpPost postRequest = new HttpPost(uri);
	    	postRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("id", userId));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(postRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 404: throw new ServerAPICallFailedException("Meeting or user id not found ('" + meetingId + "', '" + userId + "')"); // TODO Detect which is not found
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}
	}

	// PUT
	
	/**
	 * Set the status of the given user in the given meeting to "ready"
	 * 
	 * @param meetingId - The ID of the meeting to change
	 * @param userId - The ID of the user to change
	 * @throws ServerAPICallFailedException
	 */
	public void ready(int meetingId, String userId) throws ServerAPICallFailedException {
		final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("meeting/" + meetingId + "/participant/" + userId);
	    	HttpPut putRequest = new HttpPut(uri);
	    	putRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("status", "ready"));
			putRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(putRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 400: throw new ServerAPICallFailedException("User ('" + userId + "') is not a member of meeting ('" + meetingId + "')");
	    	case 404: throw new ServerAPICallFailedException("Meeting or user id not found ('" + meetingId + "', '" + userId + "')"); // TODO Detect which is not found
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}
	}
	
	// DELETE

	/**
	 * Remove the given user from the given meeting
	 * 
	 * @param meetingId - The ID of the meeting to change
	 * @param userId - The ID of the user to remove
	 * @throws ServerAPICallFailedException
	 */
	public void removeMeetingParticipant(int meetingId, String userId) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(IMReady.SERVER_URI).resolve("meeting/" + meetingId + "/participant/" + userId);
	    	HttpDelete deleteRequest = new HttpDelete(uri);
	    	deleteRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	
	    	HttpResponse response = http.execute(deleteRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 404: throw new ServerAPICallFailedException("Meeting or user id not found ('" + meetingId + "', '" + userId + "')"); // TODO Detect which is not found
	    	case 500: throw new ServerAPICallFailedException("Internal error on server");
	    	default: throw new ServerAPICallFailedException("Server returned unknown error: " + status);
	    	}
		} catch (URISyntaxException e) {
			throw new ServerAPICallFailedException("[Internal] Server URI invalid", e);
		} catch (UnsupportedEncodingException e) {
			throw new ServerAPICallFailedException("[Internal] Unsupported character encoding for form values", e);
	   	} catch (IOException e) {
	   		throw new ServerAPICallFailedException("Server returned an invalid response", e);
    	} finally {
			http.close();
		}
	}
}
