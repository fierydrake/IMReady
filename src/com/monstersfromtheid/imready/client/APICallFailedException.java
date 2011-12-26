package com.monstersfromtheid.imready.client;

public class APICallFailedException extends Exception {
	private static final long serialVersionUID = 1;

	public APICallFailedException(String msg) {
		super(msg);
	}

	public APICallFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
