package com.monstersfromtheid.imready.client;

public class ServerAPICallFailedException extends Exception {
	private static final long serialVersionUID = 1;

	public ServerAPICallFailedException(String msg) {
		super(msg);
	}

	public ServerAPICallFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
