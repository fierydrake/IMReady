package com.monstersfromtheid.imready.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CheckMeetingsAlarmReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		CheckMeetingsService.acquireLock(context);
	    
	    context.startService(new Intent(context, CheckMeetingsService.class));
	}

}
