package com.realitycheckpoint.imready.client;

public class Participant {
	private User user;
	private Meeting meeting;
	private boolean ready;
	
	public Participant(User user, Meeting meeting, boolean ready) {
		this.user = user;
		this.meeting = meeting;
		this.ready = ready;
	}
	
	public Meeting getMeeting() { return meeting; }
	public User getUser() { return user; }
	public boolean isReady() { return ready; }
}
