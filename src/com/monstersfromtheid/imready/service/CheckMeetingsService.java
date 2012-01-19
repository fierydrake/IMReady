package com.monstersfromtheid.imready.service;

import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.PowerManager;

import com.monstersfromtheid.imready.DefineAccount;
import com.monstersfromtheid.imready.IMReady;
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

	protected void checkMeetingsInBackground(Intent intent){
		// TODO - does this checking work?
		// If background data settings is off then do nothing.
		ConnectivityManager cmgr = (ConnectivityManager) getSystemService (Context.CONNECTIVITY_SERVICE);
		if( ! cmgr.getBackgroundDataSetting() ) {
			return;
		}
		// For ICE_CREAM_SANDWICH, this always returns true...
		//final 
        //if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() &&    conMgr.getActiveNetworkInfo().isConnected()) {

		if( cmgr.getActiveNetworkInfo() == null || !cmgr.getActiveNetworkInfo().isAvailable() || !cmgr.getActiveNetworkInfo().isConnected() ) {
		//if( ! ((ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isAvailable() ){
			return;
		}

		if( api == null ){
			if( IMReady.isAccountDefined(this) ){
				userName = IMReady.getUserName(this);
				api = new ServerAPI(userName);
			}
		} else {
			try {
				String latestJSON = api.userMeetings(api.getRequestingUserId());

				if( ! IMReady.getKnownMeetingsJSON(this).equalsIgnoreCase(latestJSON) ) {
					String notificationMessage = "";
					// Something's changed! Find all differences
					List <Meeting> knownMeetings  = MessageAPI.userMeetings(IMReady.getKnownMeetingsJSON(this));
					List <Meeting> latestMeetings = MessageAPI.userMeetings(latestJSON);

					// Find the new meetings
					List <Meeting> newMeetings = IMReady.newMeetings(knownMeetings, latestMeetings);

					// For all known meetings, look for differences
					List <Meeting> changedMeetings = IMReady.changedMeetings(knownMeetings, latestMeetings);

					if( newMeetings.isEmpty() ) {
						if( changedMeetings.isEmpty() ) {
							notificationMessage = "Something changed but I don't know what!";
						} else {
							notificationMessage = "Upated meetings";
						}
					} else {
						if( changedMeetings.isEmpty() ) {
							notificationMessage = "New Meeting";
						} else {
							notificationMessage = "New and updated meetings";
						}
					}

					IMReady.setKnownMeetingsJSON(latestJSON, this);

					NotificationManager notMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
					
					Notification notification = new Notification(R.drawable.notification, "IMReady Meetings", System.currentTimeMillis());
					notification.flags |= Notification.FLAG_AUTO_CANCEL;
					notification.defaults |= Notification.DEFAULT_LIGHTS;
					
					Context context = getApplicationContext();
					CharSequence contentTitle = "IMReady";
					Intent notificationIntent = new Intent(this, DefineAccount.class);
					PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, contentTitle, notificationMessage, contentIntent);
					
					notMgr.notify(NOTIFICATION_ID, notification);
				}
				
			} catch (ServerAPICallFailedException e) {
				// Silently ignore failures to get meetings.
			} catch (MessageAPIException e) {
				// Silently ignore failures to get meetings.
			}
		}
	}

	protected void onHandleIntent(Intent intent) {
		try {
			checkMeetingsInBackground(intent);
		} finally {
			getLock(this).release();
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
