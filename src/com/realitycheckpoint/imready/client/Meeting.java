package com.realitycheckpoint.imready.client;

import java.util.Collections;
import java.util.List;

public class Meeting {
    private int id;
    private String name;
    private List<Participant> participants;

    public Meeting(int id, String name, List<Participant> participants) {
        this.id = id;
        this.name = name;
        this.participants = Collections.unmodifiableList(participants);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<Participant> getParticipants() { return participants; }
}
