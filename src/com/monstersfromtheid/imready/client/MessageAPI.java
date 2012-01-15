package com.monstersfromtheid.imready.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MessageAPI {

	public static List<Meeting> userMeetings(String userMeetingsJSON) throws MessageAPIException {
	    
		try {
			ArrayList<Meeting> meetings = new ArrayList<Meeting>();
			JSONArray meetingsJSON = (JSONArray)new JSONTokener(userMeetingsJSON).nextValue();
			for (int j=0; j<meetingsJSON.length(); j++) {
				JSONObject meetingJSON = meetingsJSON.getJSONObject(j);
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
				meetings.add(new Meeting(meetingJSON.getInt("id"), meetingJSON.getString("name"), meetingJSON.getInt("state"), participants));
				}
			return meetings;
		} catch (JSONException e) {
			e.printStackTrace();
			throw new MessageAPIException("JSON invalid: " + e, e);
		}
	   
	}

}
