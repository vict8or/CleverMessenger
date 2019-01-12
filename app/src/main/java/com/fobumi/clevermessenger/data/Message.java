package com.fobumi.clevermessenger.data;

public class Message {
    private String contents;
    private long timeCreated;

    // Flag to identify if sent or received message, so MessageAdapter and show correct view
    private boolean received;

    public Message(String contents, long timeCreated, boolean received) {
        this.contents = contents;
        this.timeCreated = timeCreated;
        this.received = received;
    }

    public String getContents() {
        return contents;
    }

    public boolean isReceived() {
        return received;
    }

    public long getTimeCreated() {
        return timeCreated;
    }
}

