package com.realitycheckpoint.imready.client;

public class User {
    private String id;
    private String defaultNickname;

    public User(String id, String defaultNickname) {
        this.id = id;
        this.defaultNickname = defaultNickname;
    }

    public String getId() { return id; }
    public String getDefaultNickname() { return defaultNickname; }
}
