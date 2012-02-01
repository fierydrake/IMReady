package com.monstersfromtheid.imready.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.monstersfromtheid.imready.IMReady;

public class CheckMeetingsBootReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent arg1) {
		if( IMReady.getNotificationLevel(new ContextWrapper(context)) != 0 ) {
			IMReady.setNextAlarm(context);
		}
	}
}
