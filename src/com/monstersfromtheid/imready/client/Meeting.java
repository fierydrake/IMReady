package com.monstersfromtheid.imready.client;

import java.util.Collections;
import java.util.List;

public class Meeting {
	public static final int STATE_NOT_READY = 0;
    public static final int STATE_READY = 1;
 
    private int id;
    private String name;
    private int state;
    private List<Participant> participants;

    public Meeting(int id, String name, int state, List<Participant> participants) {
        this.id = id;
        this.name = name;
        this.state = state;
        if (participants != null) {
        	this.participants = Collections.unmodifiableList(participants);
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getState() { return state; }
    public List<Participant> getParticipants() { return participants; }
}
