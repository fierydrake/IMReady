package com.monstersfromtheid.imready.client;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Meeting {
	public static final int STATE_NOT_READY = 0;
    public static final int STATE_READY = 1;
 
    private int id;
    private String name;
    private int state;
    private ArrayList<Participant> participants;
    private boolean decorated;
    private boolean newToUser;
    private boolean changedToUser;

    public Meeting(int id, String name, int state, ArrayList<Participant> participants, 
    		boolean decorated, boolean newToUser, boolean changedToUser) {
        this.id = id;
        this.name = name;
        this.state = state;
        if (participants != null) {
        	this.participants = participants;
        }
        this.decorated = decorated;
        this.newToUser = newToUser;
        this.changedToUser = changedToUser;
    }
    
    @Override
    public boolean equals(Object o) {
    	return (this.id == ((Meeting)o).id)
    	    && (this.name.equals(((Meeting)o).name))
    	    && (this.state == ((Meeting)o).state)
    	    && (this.participants.equals( ((Meeting)o).participants ) );
    }

    public JSONObject toJSON() {
    	JSONObject meetingObject = new JSONObject();
    	try{
    		meetingObject.put("id", id);
    		meetingObject.put("name", name);
    		meetingObject.put("state", state);
    		JSONArray participantList = new JSONArray();
    		Iterator<Participant> iter = participants.iterator();
    		while(iter.hasNext()){
    			participantList.put(iter.next().toJSON());
    		}
    		meetingObject.put("participants", participantList);
    		meetingObject.put("decorated", decorated);
    		meetingObject.put("newToUser", newToUser);
    		meetingObject.put("changedToUser", changedToUser);
    	} catch (JSONException e) {
			// TODO: handle exception
		}

    	return meetingObject;
    }

    public void setParticipants(ArrayList<Participant> participants) {
    	if (this.participants != null) {
    		this.participants = participants;
    	}
    }

    public void setName(String s) { this.name = s; }
    public void setState(int i) { this.state = i; }
    public void setDecorated(boolean b) { this.decorated = b; }
    public void setNewToUser(boolean b) { this.newToUser = b; }
    public void setChangedToUser(boolean b) { this.changedToUser = b; }
    public int getId() { return id; }
    public String getName() { return name; }
    public int getState() { return state; }
    public ArrayList<Participant> getParticipants() { return participants; }
    public boolean isDecorated() { return decorated; }
    public boolean isNewToUser() { return newToUser; }
    public boolean isChangedToUser() { return changedToUser; }
}
