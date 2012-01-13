package com.monstersfromtheid.imready;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

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
}
