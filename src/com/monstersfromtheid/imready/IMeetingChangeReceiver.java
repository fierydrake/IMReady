package com.monstersfromtheid.imready;

public interface IMeetingChangeReceiver {
	public static final String ACTION_RESP = "com.monstersfromtheid.imready.MEETING_CHANGES";
	
	abstract void processMeetingsChange();
}
