package com.example.messenger;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.ArrayList;
import com.google.firebase.firestore.PropertyName;

public class Chat {
    private String id;
    private List<String> participants;
    private String otherUserName;
    private String lastMessageText;
    private Timestamp lastMessageTime;
    private boolean isGroup = false;
    private String groupName;
    private List<String> favourite = new ArrayList<>();
    private java.util.Map<String, Long> unreadCounts = new java.util.HashMap<>();

    public Chat() {
        // Required empty constructor for Firestore
    }

    public Chat(List<String> participants, String lastMessageText, Timestamp lastMessageTime) {
        this.participants = participants;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.isGroup = participants.size() > 2; // Set isGroup based on number of participants
    }

    public Chat(List<String> participants, String lastMessageText, Timestamp lastMessageTime, boolean isGroup, String groupName) {
        this.participants = participants;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.isGroup = isGroup;
        this.groupName = groupName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public Timestamp getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    @PropertyName("groupName")
    public String getGroupName() {
        return groupName;
    }

    @PropertyName("groupName")
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getFavourite() {
        return favourite;
    }

    public void setFavourite(List<String> favourite) {
        this.favourite = favourite;
    }

    public java.util.Map<String, Long> getUnreadCounts() {
        return unreadCounts;
    }

    public void setUnreadCounts(java.util.Map<String, Long> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }
}