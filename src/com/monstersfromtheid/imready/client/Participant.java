package com.monstersfromtheid.imready.client;

import org.json.JSONException;
import org.json.JSONObject;

public class Participant {
    public static final int STATE_NOT_READY = 0;
    public static final int STATE_READY = 1;
    private User user;
    private int state;
    private boolean notified;

    public Participant(User user, int state, boolean notified) {
        this.user = user;
        this.state = state;
        this.notified = notified;
    }
    
    @Override
    public boolean equals(Object o) {
    	return ( this.state == ((Participant)o).state )
    		&& ( this.notified == ((Participant)o).notified )
    		&& ( this.user.equals( ((Participant)o).user ));
    }
    
    public JSONObject toJSON() {
    	JSONObject participantObject = new JSONObject();
    	try{
    		// {"id": "dave123", "defaultNickname": "Dave", "state": 0, "notified": true }
    		participantObject.put("id", user.getId());
    		participantObject.put("defaultNickname", user.getDefaultNickname());
    		participantObject.put("state", state);
    		participantObject.put("notified", notified);
    	} catch (JSONException e) {
			// TODO: handle exception
		}
    	
    	return participantObject;
    }

    public User getUser() { return user; }
    public int getState() { return state; }
    public boolean getNotified() { return notified; }
}
