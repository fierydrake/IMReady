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
				e.printStackTrace();
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
	
	private static final String SERVER_URI = "http://imready.monstersfromtheid.co.uk:54321/"; 
	
	private String requestingUserId;
	
	public ServerAPI(String requestingUserId) {
		this.requestingUserId = requestingUserId;
	}
	public String getRequestingUserId() { return requestingUserId; }
	
	// GET
	public void user(String id) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("user/" + id);
	    	HttpGet getRequest = new HttpGet(uri);
	    	getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	HttpResponse response = http.execute(getRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	switch (status) {
	    	case 200: return; // OK
	    	case 404: throw new ServerAPICallFailedException("User id '" + id + "' not found");
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
	
	public String meeting(int id) throws ServerAPICallFailedException {
	    final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
	    try {
	        URI uri = new URI(SERVER_URI).resolve("meeting/" + id);
	        HttpGet getRequest = new HttpGet(uri);
	        getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	        HttpResponse response = http.execute(getRequest);

	        int status = response.getStatusLine().getStatusCode();
	        switch (status) {
	        case 200: // OK
	            String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	            try {
	            	// TODO - check return is valid JSON?

	            	JSONObject meetingJSON = (JSONObject)new JSONTokener(body).nextValue();
	                return body;
	            } catch (IllegalArgumentException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            } catch (JSONException e) {
	                e.printStackTrace();
	                throw new ServerAPICallFailedException("Server response invalid: " + e, e);
	            }
	        case 404: throw new ServerAPICallFailedException("Meeting with id '" + id + "' not found");
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

	public String userMeetings(String userId) throws ServerAPICallFailedException {
	    final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
	    try {
	        URI uri = new URI(SERVER_URI).resolve("meetings/" + userId);
	        HttpGet getRequest = new HttpGet(uri);
	        getRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	        HttpResponse response = http.execute(getRequest);

	        int status = response.getStatusLine().getStatusCode();
	        switch (status) {
	        case 200: // OK
	            String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent()), 2048).readLine();
	            try {
	            	// TODO - check return is valid JSON?

	            	JSONArray meetingsJSON = (JSONArray)new JSONTokener(body).nextValue();
	            	return body;
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
	
//	public List<Participant> meetingParticipants(int meetingId) throws APICallFailedException { 
//		return null; 
//	}
	
	// POST
	public void createUser(String id, String defaultNickname) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("users");
	    	HttpPost postRequest = new HttpPost(uri);
	    	postRequest.addHeader("X-IMReady-Auth-ID", getRequestingUserId());
	    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("id", id));
			nameValuePairs.add(new BasicNameValuePair("defaultNickname", defaultNickname));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	    	HttpResponse response = http.execute(postRequest);

	    	int status = response.getStatusLine().getStatusCode();
	    	String msg = response.getEntity().toString(); // FIXME
	    	switch (status) {
	    	case 200: return; // OK
	    	case 400: throw new ServerAPICallFailedException(msg + " '" + id + "'");
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
	
	public int createMeeting(String creatorId, String name) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
    	String body = null;
    	String id = null;
		try {
			URI uri = new URI(SERVER_URI).resolve("meetings");
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
	public void addMeetingParticipant(int meetingId, String userId) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("meeting/" + meetingId + "/participants");
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
	public void ready(int meetingId, String userId) throws ServerAPICallFailedException {
		final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("meeting/" + meetingId + "/participant/" + userId);
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
	public void removeMeetingParticipant(int meetingId, String userId) throws ServerAPICallFailedException {
    	final AndroidHttpClient http = AndroidHttpClient.newInstance(IMReady.CLIENT_HTTP_NAME);
		try {
			URI uri = new URI(SERVER_URI).resolve("meeting/" + meetingId + "/participant/" + userId);
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