package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import com.monstersfromtheid.imready.client.Meeting;

public class IMReady {
	public static final String PREFERENCES_NAME = "IMReadyPrefs";
	
	public static final String CLIENT_HTTP_NAME = "Android ImReady 0.1";
	
	public static final String ACTIONS_ACOUNT_CHANGE_DETAILS = "accountDetailsChange";
	
	public static final String RETURNS_MEETING_ID   = "MeetingID";
	public static final String RETURNS_MEETING_NAME = "MeetingName";
	
	private static final String PREFERENCES_KEYS_ACCOUNT_DEFINED = "accountDefined";
	private static final String PREFERENCES_KEYS_USERNAME        = "accountUserName";
	private static final String PREFERENCES_KEYS_NICKNAME        = "accountNickName";
	private static final String PREFERENCES_KEYS_MEETING_JSON    = "knownMeetingsJSON";
	
	
	public static final boolean isAccountDefined(ContextWrapper c){
		return c.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(PREFERENCES_KEYS_ACCOUNT_DEFINED, false);
	}
	
	public static final void setAccountDefined(boolean defined, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putBoolean(PREFERENCES_KEYS_ACCOUNT_DEFINED, defined);
        preferences.commit();
	}

	public static final String getUserName(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_USERNAME, "");
	}
	
	public static final String getNickName(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_NICKNAME, "");
	}
	
	public static final void setUserAndNickName(String userName, String nickName, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putString(PREFERENCES_KEYS_USERNAME, userName);
        preferences.putString(PREFERENCES_KEYS_NICKNAME, nickName);
        preferences.commit();
	}
	
	public static final String getKnownMeetingsJSON(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_MEETING_JSON, "");
	}
	
	public static final void setKnownMeetingsJSON(String meetings, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putString(PREFERENCES_KEYS_MEETING_JSON, meetings);
        preferences.commit();
	}
	
	public static final List<Meeting> newMeetings(List<Meeting> knownMeetings, List<Meeting> latestMeetings){
		ArrayList <Meeting> newMeetings = new ArrayList<Meeting>();
		
		Iterator<Meeting> iterLatest = latestMeetings.iterator();
		while(iterLatest.hasNext()){
			boolean newMeetingFlag = true;
			Meeting thisLatestMeeting = (Meeting)iterLatest.next();
			int thisLatestMeetingId = thisLatestMeeting.getId();

			Iterator<Meeting> iterKnown = knownMeetings.iterator();
			while(iterKnown.hasNext()){
				int thisKnownMeetingId = ((Meeting)iterKnown.next()).getId();
				
				if( thisLatestMeetingId == thisKnownMeetingId ) {
					newMeetingFlag = false;
				}
			}
			
			if(newMeetingFlag){
				newMeetings.add(thisLatestMeeting);
			}
		}
		
		return newMeetings;
	}
	
	public static final List<Meeting> changedMeetings(List<Meeting> knownMeetings, List<Meeting> latestMeetings){
		ArrayList <Meeting> changedMeetings = new ArrayList<Meeting>();
		
		Iterator<Meeting> iterKnown = knownMeetings.iterator();
		while(iterKnown.hasNext()){
			Meeting thisKnownMeeting = (Meeting)iterKnown.next();
			int thisKnownMeetingId = thisKnownMeeting.getId();

			Iterator<Meeting> iterLatest = latestMeetings.iterator();
			while(iterLatest.hasNext()){
				Meeting thisLatestMeeting = ((Meeting)iterLatest.next());
				int thisLatestMeetingId = thisLatestMeeting.getId();

				if( thisLatestMeetingId == thisKnownMeetingId ) {
					if( ! thisKnownMeeting.equals(thisLatestMeeting) ){
						changedMeetings.add(thisLatestMeeting);
					}
				}
			}
		}
		
		return changedMeetings;
	}
}

