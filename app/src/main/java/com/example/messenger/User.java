package com.example.messenger;

public class User {
    private String uid;
    private String email;
    private String name;
    private boolean isOnline;
    private long lastSeen;

    public User() {
        // Required empty constructor for Firestore
    }

    public User(String uid, String email, String name) {
        this.uid = uid;
        this.email = email;
        this.name = name != null && !name.trim().isEmpty() ? name : email.split("@")[0];
        this.isOnline = false;
        this.lastSeen = System.currentTimeMillis();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name != null && !name.trim().isEmpty() ? name : email.split("@")[0];
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}