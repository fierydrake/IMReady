package com.monstersfromtheid.imready.client;

public class MessageAPIException extends Exception {
	private static final long serialVersionUID = 1;

	public MessageAPIException(String msg) {
		super(msg);
	}

	public MessageAPIException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
