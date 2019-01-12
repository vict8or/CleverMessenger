package com.fobumi.clevermessenger.data;

public class Contact {
    // Primary display name in contacts database
    private String name;
    private String lookupKey;

    public Contact(String name, String lookupKey) {
        this.name = name;
        this.lookupKey = lookupKey;
    }

    // Used as to key obtain phone number of this contact
    public String getLookupKey() {
        return lookupKey;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
