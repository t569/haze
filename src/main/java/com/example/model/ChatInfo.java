package com.example.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents detailed information for a chat message.
 */
@Entity
@Table(name = "chat_infos")
public class ChatInfo {

    /** Primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Timestamp of the message */
    @Column(nullable = false)
    private Instant timestamp;

    /** The user who sent this message */
    @ManyToOne(optional = false)
    @JoinColumn(name = "sent_by_user_id", referencedColumnName = "id")
    private User sentBy;

    /** Message text content */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Many messages → One Chat */
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private Chat chat;

    /** Default constructor */
    public ChatInfo() { }

    /** All-args constructor */
    public ChatInfo(Instant timestamp, User sentBy, String text) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.text = text;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public User getSentBy() {
        return sentBy;
    }

    public void setSentBy(User sentBy) {
        this.sentBy = sentBy;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }
}
