package com.monstersfromtheid.imready.client;

public class Participant {
    public static final int STATE_NOT_READY = 0;
    public static final int STATE_READY = 1;
    private User user;
    private int state;
    private boolean notified;

    public Participant(User user, int state, boolean notified) {
        this.user = user;
        this.state = state;
        this.notified = notified;
    }

    public User getUser() { return user; }
    public int getState() { return state; }
    public boolean getNotified() { return notified; }
}
