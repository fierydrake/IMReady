package com.monstersfromtheid.imready.service;

import com.monstersfromtheid.imready.DefineAccount;
import com.monstersfromtheid.imready.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class CheckMeetingsService extends IntentService {

	private static PowerManager.WakeLock lock = null;
	public static final String LOCK_NAME = "com.monstersfromtheid.imready.service.CheckMeetingsService";
	private static final int HELLO_ID = 1;
	
	public CheckMeetingsService(String name) {
		super(name);
	}


	protected void checkMeetings(Intent intent){
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
			checkMeetings(intent);
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
