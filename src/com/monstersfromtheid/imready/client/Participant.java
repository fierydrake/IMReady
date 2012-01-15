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
    
    @Override
    public boolean equals(Object o) {
    	return ( this.state == ((Participant)o).state )
    		&& ( this.notified == ((Participant)o).notified )
    		&& ( this.user.equals( ((Participant)o).user ));
    }

    public User getUser() { return user; }
    public int getState() { return state; }
    public boolean getNotified() { return notified; }
}
