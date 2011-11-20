package com.realitycheckpoint.imready.client;

public class Meeting {
	private int id;
	private String name;
	
	public Meeting(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public int getId() { return id; }
	public String getName() { return name; }
}
