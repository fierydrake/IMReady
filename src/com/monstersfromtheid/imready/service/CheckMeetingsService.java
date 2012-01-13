package com.monstersfromtheid.imready.service;

import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import com.monstersfromtheid.imready.DefineAccount;
import com.monstersfromtheid.imready.IMReady;
import com.monstersfromtheid.imready.R;
import com.monstersfromtheid.imready.client.API;
import com.monstersfromtheid.imready.client.APICallFailedException;
import com.monstersfromtheid.imready.client.Meeting;

public class CheckMeetingsService extends IntentService {

	private static PowerManager.WakeLock lock = null;
	public static final String LOCK_NAME = "com.monstersfromtheid.imready.service.CheckMeetingsService";
	private static final int HELLO_ID = 1;
	private static API api;
	private static String userName;
	
	public CheckMeetingsService(String name) {
		super(name);
		if( IMReady.isAccountDefined(this) ){
			userName = IMReady.getUserName(this);
			api = new API(userName);
		}
	}

	public CheckMeetingsService() {
		this("CheckMeetingsService");
	}

	protected void checkMeetingsInBackground(Intent intent){
		// TODO - If background data settings is off then do nothing.
		if( api == null ){
			if( IMReady.isAccountDefined(this) ){
				userName = IMReady.getUserName(this);
				api = new API(userName);
			}
		} else {
			try {
				api.userMeetings(api.getRequestingUserId());
				//if( ! IMReady.getKnownMeetingsJSON(this).equalsIgnoreCase(api.userMeetingsJSON(api.getRequestingUserId()) ) {
					// Something's changed!
				    // Find all differences
				    // Notifications for differences.
				    // Update -> IMReady.setKnownMeetingsJSON("string", this);
				//}
				
			} catch (APICallFailedException e) {
				// Silently ignore failures to get meetings.
			}
		}
		
		
		NotificationManager notMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		
		Notification notification = new Notification(R.drawable.notification, "Timer fired", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "IMReady Event";
		CharSequence contentText = "The Timer has now fired!";
		Intent notificationIntent = new Intent(this, DefineAccount.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		notMgr.notify(HELLO_ID, notification);
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
