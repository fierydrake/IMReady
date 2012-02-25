package com.monstersfromtheid.imready.service;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.PowerManager;

import com.monstersfromtheid.imready.IMReady;
import com.monstersfromtheid.imready.MyMeetings;
import com.monstersfromtheid.imready.R;
import com.monstersfromtheid.imready.client.Meeting;
import com.monstersfromtheid.imready.client.MessageAPI;
import com.monstersfromtheid.imready.client.MessageAPIException;
import com.monstersfromtheid.imready.client.ServerAPI;
import com.monstersfromtheid.imready.client.ServerAPICallFailedException;

public class CheckMeetingsService extends IntentService {

	private static PowerManager.WakeLock lock = null;
	public static final String LOCK_NAME = "com.monstersfromtheid.imready.service.CheckMeetingsService";
	private static final int NOTIFICATION_ID = 1;
	private static ServerAPI api;
	private static String userName;
	
    public class LocalBinder extends Binder {
        public CheckMeetingsService getService() {
            return CheckMeetingsService.this;
        }
    }
    
    /*
     * Andy: Mike, you might want to ask me something *knowing grin*
     */
    public static class MeetingsChangeEvent extends EventObject {
		private static final long serialVersionUID = 1L;

		public MeetingsChangeEvent(Object source) {
			super(source);
		}
	}
    public interface MeetingsChangeListener {
    	public void onMeetingsChange(MeetingsChangeEvent e);
    }
    List<MeetingsChangeListener> listeners = new ArrayList<MeetingsChangeListener>(2);
    public void addMeetingsChangeListener(MeetingsChangeListener listener) {
    	listeners.add(listener);
    }
    public void removeMeetingsChangeListener(MeetingsChangeListener listener) {
    	listeners.remove(listener);
    }
    public void fireMeetingsChanged(MeetingsChangeEvent e){
    	for (MeetingsChangeListener listener : listeners) {
    		listener.onMeetingsChange(e);
    	}
    }
    public static class MeetingUpdate {
    	public enum UpdateType { NEW, CHANGE };
    	
    	private Meeting updatedMeeting;
    	private UpdateType updateType;
    	
    	public MeetingUpdate(Meeting updatedMeeting, UpdateType updateType) {
    		this.updatedMeeting = updatedMeeting;
    		this.updateType = updateType;
    	}
    	
    	public UpdateType getUpdateType() { return updateType; }
    	public Meeting getUpdatedMeeting() { return updatedMeeting; }
    }
    // TODO: Add 1 OR 2 public methods that the UI activities can call
    //       to get (a) the status quo, (b) the meeting changes since last "rollup"
    public synchronized List<Meeting> getMeetings() throws MessageAPIException { /* rethrow a more appropriate exception? */
    	return MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));
    }
    public synchronized List<MeetingUpdate> getMeetingUpdates() throws MessageAPIException { /* rethrow a more appropriate exception? */
    	List<Meeting> userAwareMeetings = MessageAPI.userMeetings(IMReady.getUserAwareMeetingsJSON(this));
    	List<Meeting> lastSeenMeetings = MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));
    	
    	List<Meeting> newMeetings = IMReady.newMeetings(userAwareMeetings, lastSeenMeetings);
    	List<Meeting> changedMeetings = IMReady.changedMeetings(userAwareMeetings, lastSeenMeetings);
    	List<MeetingUpdate> updates = new ArrayList<MeetingUpdate>(newMeetings.size() + changedMeetings.size());
    	
    	for (Meeting newMeeting : newMeetings) { updates.add(new MeetingUpdate(newMeeting, MeetingUpdate.UpdateType.NEW)); }
    	for (Meeting changedMeeting : changedMeetings) { updates.add(new MeetingUpdate(changedMeeting, MeetingUpdate.UpdateType.CHANGE)); }
    	//IMReady.readyMeetings(meetings); // ? Surely we only want meetings that changed to ready since we last looked?
    	
    	return updates;
    }
    // TODO: Add a method to rollup the "last seen" up into the "last user aware" JSON
    public synchronized void rollupMeetingUpdates() { rollupMeetingUpdates(IMReady.getLastSeenMeetingsJSON(this)); }
    public synchronized void rollupMeetingUpdates(String lastSeenMeetingsJSON) {
    	IMReady.setUserAwareMeetingsJSON(lastSeenMeetingsJSON, this);
    }
    
	public CheckMeetingsService(String name) {
		super(name);
	}

	public CheckMeetingsService() {
		this("CheckMeetingsService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		if( IMReady.isAccountDefined(this) ){
			userName = IMReady.getUserName(this);
			api = new ServerAPI(userName);
		}
	}

	/**
	 * Try and get the latest meeting info from the server.
	 * If something goes wrong, then return the last recorded data.
	 * 
	 * @param intent
	 * @return
	 */
	protected String getLatestMeetings(Intent intent){
		if( api == null && IMReady.isAccountDefined(this) ){
			userName = IMReady.getUserName(this);
			api = new ServerAPI(userName);
		}
		
		try {
			return api.userMeetings(api.getRequestingUserId());
		} catch (ServerAPICallFailedException e) {
			// Silently ignore failures to get meetings.
			return IMReady.getLastSeenMeetingsJSON(this);
		}
	}
	
	/**
	 * Compare the provided JSON with the last seen JSON.  Use any differences (and the
	 * current notification level) to generate notifications of the changes
	 * 
	 * @param latestJSON
	 */
	protected void generateNotifications(String latestJSON){
		int notificationLevel = IMReady.getNotificationLevel(this);
		if (notificationLevel == 0) {
			return;
		}

		// Assume the notification lasts until it is acted on by the user.
		// This means that I only create a new notification if I saw something change since I last looked.
		// There is a special case: New meeting X, delete meeting X.  In this case, I delete the notification.
		// Otherwise I try to list number of meetings that are:
		//  New     - A meeting that is previously unknown to the user.
		//  Ready   - Meeting a user was aware of that has become ready.  Note there can't be a "New" meeting
		//            that is ready as the user would have had to mark it ready, and thus be aware of it.
		//  Changed - Meetings that the user is aware of that have seen some change since they were last aware 
		//            of it.  This requires recording a list of meetings that have seen changes.
		if( ! IMReady.getLastSeenMeetingsJSON(this).equalsIgnoreCase(latestJSON) ) {
			// Something's changed since I last looked! Find all differences
			try {
				List <Meeting> userAwareMeetings  = MessageAPI.userMeetings(IMReady.getUserAwareMeetingsJSON(this));
				List <Meeting> lastSeenMeetings   = MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));
				List <Meeting> latestMeetings     = MessageAPI.userMeetings(latestJSON);

				// Find the new meetings since user was last aware
				List<Meeting> newMeetings = IMReady.newMeetings(userAwareMeetings, latestMeetings);
				int newM = newMeetings.size();

				// For all meetings the user is aware of, look for meetings that have gone ready
				List<Meeting> readyMeetings = IMReady.readyMeetings(IMReady.changedMeetings(userAwareMeetings, latestMeetings));
				int readyM = readyMeetings.size();

				// Get a list of meetings that we already know have changed plus meetings we've just found have changed.
				List<Meeting> changedMeetings = IMReady.changedMeetings(lastSeenMeetings, latestMeetings);
				List<Integer> newDirtyMeetings = IMReady.mergeDirtyMeetingList(IMReady.getDirtyMeetings(this), changedMeetings);
				IMReady.setDirtyMeetings(newDirtyMeetings, this);
				int changeM = 0;
				if( notificationLevel > 1 ){
					changeM = newDirtyMeetings.size();
				}

				IMReady.setLastSeenMeetingsJSON(latestJSON, this);

				// QUESTION Do we want to increase polling frequency when we know we have a new meeting?

				// Now we know what's changed, it's time to generate a notification
				Notification notification = new Notification(R.drawable.notification, "IMReady Meetings", System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;

				notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				notification.ledOnMS = 300;
				notification.ledOffMS = 1100;

				// [(n) new[,] ][(r) ready[ and] ][(c) changed]
				String notificationMessage = "";
				NotificationManager notMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
				if( newM > 0 ){
					if( readyM > 0 ){
						if( changeM > 0 ){
							notificationMessage = "(" + newM + ") new, (" + readyM + ") ready and (" + changeM + ") changed";
						} else {
							notificationMessage = "(" + newM + ") new and (" + readyM + ") ready";								
						}
					} else {
						if( changeM > 0 ){
							notificationMessage = "(" + newM + ") new and (" + changeM + ") changed";
						} else {
							notificationMessage = "(" + newM + ") new";								
						}
					}
				} else {
					if( readyM > 0 ){
						if( changeM > 0 ){
							notificationMessage = "(" + readyM + ") ready and (" + changeM + ") changed";
						} else {
							notificationMessage = "(" + readyM + ") ready";								
						}
					} else {
						if( changeM > 0 ){
							// Add a preferences check to see if we should display this.
							notificationMessage = "(" + changeM + ") changed";
						} else {
							// This is a special case, we've gone round a loop of changes and ended up at the exact
							// same state since the user last looked, so we cancel any notifications
							notMgr.cancel(NOTIFICATION_ID);
							return;
						}
					}
				}
				
				if (newM > 0 || readyM > 0 || changedMeetings.size() > 0) {
					fireMeetingsChanged(new MeetingsChangeEvent(this));
				}

				CharSequence notificationTitle = getString( R.string.app_name );
				Context context = getApplicationContext();
				Intent notificationIntent = new Intent(this, MyMeetings.class);
				notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
				notification.setLatestEventInfo(context, notificationTitle, notificationMessage, pendingIntent);

				notMgr.notify(NOTIFICATION_ID, notification);
			} catch (MessageAPIException e) {
				// If there is a problem understanding the JSON, fail silently.
				return;
			}
		}
	}

	protected void onHandleIntent(Intent intent) {
		try {
			setNextWakeup(intent);

			// TODO - does this checking work?
			// If background data settings is off then do nothing.
			ConnectivityManager cmgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
			if( cmgr.getBackgroundDataSetting() ) {
				// For ICE_CREAM_SANDWICH, cmgr.getBackgroundDataSetting() always returns true...
				// But the code bellow, won't soding work!
				//if( cmgr.getActiveNetworkInfo() == null || !cmgr.getActiveNetworkInfo().isAvailable() || !cmgr.getActiveNetworkInfo().isConnected() ) {
				//	return;
				//}
				generateNotifications( getLatestMeetings(intent) );
			}
		} finally {
			getLock(this).release();
		}
	}
	
	protected void setNextWakeup(Intent intent) {
		// If we're polling in dynamic mode, then set the next poll alarm.
		if( IMReady.getPollingInterval(this) == 2 ){
			IMReady.setNextAlarm(this);
		}
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lock == null) {
			PowerManager pmgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			lock = pmgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME);
			lock.setReferenceCounted(true);
		}
		return (lock);
	}

	public static void acquireLock(Context context) {
		getLock(context).acquire();
	}

}
