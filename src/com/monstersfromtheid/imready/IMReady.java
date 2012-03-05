package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.monstersfromtheid.imready.client.Meeting;
import com.monstersfromtheid.imready.service.CheckMeetingsAlarmReceiver;

public class IMReady {
	public static final String PREFERENCES_NAME = "IMReadyPrefs";
	
	public static final String CLIENT_HTTP_NAME = "Android ImReady 0.1";
	
	public static final String ACTIONS_ACOUNT_CHANGE_DETAILS = "accountDetailsChange";
	
	public static final String RETURNS_MEETING_ID   = "MeetingID";
	public static final String RETURNS_MEETING_NAME = "MeetingName";
	
	public static final String PREFERENCES_KEYS_ACCOUNT_DEFINED = "accountDefined";
	public static final String PREFERENCES_KEYS_USERNAME        = "accountUserName";
	public static final String PREFERENCES_KEYS_NICKNAME        = "accountNickName";
	public static final String PREFERENCES_KEYS_MEETING_JSON    = "knownMeetingsJSON";
	public static final String PREFERENCES_KEYS_CHANGES_JSON    = "knownMeetingsForChangesJSON";
	public static final String PREFERENCES_KEYS_DIRTY_MEETINGS  = "dirtyMeetings";
	public static final String PREFERENCES_KEYS_POL_INTERVAL    = "pollingInterval";
	public static final String PREFERENCES_KEYS_NOTIFY_LEVEL    = "notificationLevel";

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
	
	public static final int getPollingInterval(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(PREFERENCES_KEYS_POL_INTERVAL, 0);
	}

	public static final void setPollingInterval(int interval, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putInt(PREFERENCES_KEYS_POL_INTERVAL, interval);
        preferences.commit();
	}

	/**
	 * Return the current level of notification:<br>
	 * 
	 * 0 - No notifications<br>
	 * 1 - New &amp; ready meetings<br>
	 * 2 - All meeting changes<br>
	 * 
	 * @param c
	 * @return
	 */
	public static final int getNotificationLevel(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getInt(PREFERENCES_KEYS_NOTIFY_LEVEL, 1);
	}

	public static final void setNotificationLevel(int interval, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putInt(PREFERENCES_KEYS_NOTIFY_LEVEL, interval);
        preferences.commit();
	}

	public static final String getUserAwareMeetingsJSON(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_MEETING_JSON, "[]");
	}
	
	public static final void setUserAwareMeetingsJSON(String meetings, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putString(PREFERENCES_KEYS_MEETING_JSON, meetings);
        preferences.commit();
	}
	
	public static final String getLastSeenMeetingsJSON(ContextWrapper c){
		return c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_CHANGES_JSON, "[]");
	}

	public static final void setLastSeenMeetingsJSON(String meetings, ContextWrapper c){
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putString(PREFERENCES_KEYS_CHANGES_JSON, meetings);
        preferences.commit();
	}
	
	public static final List<Integer> getDirtyMeetings(ContextWrapper c){
		ArrayList<Integer> dirtyMeetings = new ArrayList<Integer>();
		
		String[] s = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).getString(PREFERENCES_KEYS_DIRTY_MEETINGS, "").split("-");
		for (int i = 0; i < s.length; i++) {
			if( s[i].length() > 0 ){
				dirtyMeetings.add( new Integer( s[i] ) );
			}
		}
		
		return dirtyMeetings;
	}
	
	public static final void setDirtyMeetings(List<Integer> dirtyMeetings, ContextWrapper c){
		String s = "";
		
		Iterator<Integer> iter = dirtyMeetings.iterator();
		while(iter.hasNext()){
			s += ((Integer)iter.next());
			if(iter.hasNext()) {
				s += "-";
			}
		}
		
		SharedPreferences.Editor preferences = c.getSharedPreferences(IMReady.PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        preferences.putString(PREFERENCES_KEYS_DIRTY_MEETINGS, s);
        preferences.commit();
	}
	
	public static final void setNextAlarm(Context context){
		setNextAlarm(context, false);
	}

	/**
	 * Set the next alarm.  Use the current time, the polling interval settings and the 
	 * notification level to work out when to set the alarm for, if at all. 
	 * 
	 * If the notification level is zero, then any existing alarm is cancelled.
	 * 
	 * @param context
	 * @param connected - Is an Activity running at the moment? 
	 */
	public static final void setNextAlarm(Context context, boolean connected){
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    Intent i = new Intent(context, CheckMeetingsAlarmReceiver.class);
	    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

	    // If we're not connected and notifications are off, always turn off the alarm 
	    if( !connected && getNotificationLevel(new ContextWrapper(context)) == 0 ){
	    	alarm.cancel(pi);
	    	return;
	    }

		// Should checks for whether to call go in here or out at the caller level?
		//  are we in dynamic mode?
	    // TODO - add the check to see if anything needs to be done.

		long interval;
		if(connected) {
			interval = 20000;
		} else {
			switch ( getPollingInterval(new ContextWrapper(context)) ) {
			case 0:
				interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
				break;

			case 1:
				interval = AlarmManager.INTERVAL_HOUR;
				break;

			case 2:
				int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
				interval = ( 8 < currentHour && currentHour < 23 ) ? AlarmManager.INTERVAL_FIFTEEN_MINUTES : AlarmManager.INTERVAL_HOUR;
				break;

			default:
				interval = AlarmManager.INTERVAL_HOUR;
				break;
			}
		}
		
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime()+1000,
				interval,
				pi);
	
	// TODO Ideally want to know if we just crossed a boundary and only fiddle the alarm then.
	// Push this into another class and then all routes through code can just say setupAlarm.
	
    //alarm.cancel(pi);
	}

	/**
	 * Look through the list of <b>latestMeetings</b> and return 
	 * a list of all Meeting objects that are not in <b>knownMeetings</b>.
	 * 
	 * @param knownMeetings
	 * @param latestMeetings
	 * @return
	 */
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
	
	/**
	 * For each of the <b>knownMeetings</b>, look in the list of 
	 * <b>latestMeetings</b> and return a list of those Meetings that
	 * have changed.
	 *   
	 * @param knownMeetings
	 * @param latestMeetings
	 * @return
	 */
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
	
	/** 
	 * Return a list of the Meeting objects in <b>meetings</b> that are 
	 * marked as ready.
	 * 
	 * @param meetings
	 * @return
	 */
	public static final List<Meeting> readyMeetings(List<Meeting> meetings){
		ArrayList <Meeting> readyMeetings = new ArrayList<Meeting>();

		Iterator<Meeting> iterM = meetings.iterator();
		while(iterM.hasNext()){
			Meeting thisMeeting = (Meeting)iterM.next();
			if ( thisMeeting.getState() == 1 ){
				readyMeetings.add(thisMeeting);
			}
		}

		return readyMeetings;
	}
	
	/**
	 * Return a list of Integers containing the union of <b>knownDirt</b> and the 
	 * meeting ID numbers in <b>newDirt</b>.  Note that any entries in <b>newDirt</b>
	 * that are in a ready state are removed from <b>knownDirt</b>.
	 * 
	 * @param knownDirt
	 * @param newDirt
	 * @return
	 */
	public static final List<Integer> mergeDirtyMeetingList(List<Integer> knownDirt, List<Meeting> newDirt){
		ArrayList <Integer> fullDirt = new ArrayList<Integer>(knownDirt);

		Iterator<Meeting> iterNew = newDirt.iterator();
		while(iterNew.hasNext()){
			Meeting m = (Meeting)iterNew.next();
			Integer i = new Integer( m.getId() );
			// If the change isn't already in our list, and the change isn't for a ready meeting, add it
			if( ! knownDirt.contains(i) && m.getState() != 1 ){
				fullDirt.add(i);
			}
			// If the change is already in our list, but the meeting is now ready, remove from 
			// the change list; this will already be counted as a ready meeting
			if( fullDirt.contains(i) && m.getState() == 1 ){
				fullDirt.remove(i);
			}
		}

		return fullDirt;
	}

}

