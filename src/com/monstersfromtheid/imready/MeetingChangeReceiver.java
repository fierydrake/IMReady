package com.monstersfromtheid.imready;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MeetingChangeReceiver extends BroadcastReceiver {
	
	IMeetingChangeReceiver receiver;
	
	public MeetingChangeReceiver(IMeetingChangeReceiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		receiver.processMeetingsChange();
	}

}
