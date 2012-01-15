package com.monstersfromtheid.imready.client;

public class User {
    private String id;
    private String defaultNickname;

    public User(String id, String defaultNickname) {
        this.id = id;
        this.defaultNickname = defaultNickname;
    }
    
    @Override
    public boolean equals(Object o) {
    	return this.id.equals( ((User)o).id ) && this.defaultNickname.equals( ((User)o).defaultNickname );
    }

    public String getId() { return id; }
    public String getDefaultNickname() { return defaultNickname; }
}
