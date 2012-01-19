package com.monstersfromtheid.imready.service;

import com.monstersfromtheid.imready.IMReady;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class CheckMeetingsBootReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent arg1) {
		AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	    Intent i = new Intent(context, CheckMeetingsAlarmReceiver.class);
	    PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

	    alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
	    		SystemClock.elapsedRealtime()+60000,
	    		IMReady.DEFAULT_CHECK_PERIOD,
	    		pi);
	}

}
