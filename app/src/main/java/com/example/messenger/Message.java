package com.example.messenger;

import com.google.firebase.Timestamp;

public class Message {
    private String id;
    private String chatId;
    private String senderId;
    private String text;
    private Timestamp timestamp;
    private String status; // "sent", "delivered", or "read"

    public Message() {
        // Required empty constructor for Firestore
    }

    public Message(String senderId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.status = "sent"; // Default status when message is sent
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}