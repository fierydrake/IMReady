package com.monstersfromtheid.imready.client;

public class Participant {
    public static final int STATE_NOT_READY = 0;
    public static final int STATE_READY = 1;
    private User user;
    private int state;
    private boolean notified;

    public Participant(User user, String state, String notified) {
        this(user, 0, false);
        if (state != null) {
            try {
                this.state = Integer.parseInt(state);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("state argument is not numeric", e);
            }
        } else {
            throw new IllegalArgumentException("state argument is not valid");
        }
        if ("01".contains(notified) && notified.length() == 1) {
            this.notified = "1".equals(notified);
        } else {
            throw new IllegalArgumentException("notified argument is not valid");
        }
    }
    public Participant(User user, int state, boolean notified) {
        this.user = user;
        this.state = state;
        this.notified = notified;
    }

    public User getUser() { return user; }
    public int getState() { return state; }
    public boolean getNotified() { return notified; }
}
