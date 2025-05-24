package com.example.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Many Users ↔️ Many Chats (owning side) */
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "user_chats",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "chat_id")
    )
    private Set<Chat> chats = new HashSet<>();

    public User() {}

    public User(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Chat> getChats() {
        return chats;
    }

    /**
     * Add chat to user and maintain both sides of relationship
     */
    public void addChat(Chat chat) {
        chats.add(chat);
        chat.getUsers().add(this);
    }

    /**
     * Remove chat from user and maintain both sides of relationship
     */
    public void removeChat(Chat chat) {
        chats.remove(chat);
        chat.getUsers().remove(this);
    }

    /**
     * Remove all chat associations for cleanup/deletion
     */
    public void removeAllChats() {
        for (Chat chat : new HashSet<>(chats)) {
            removeChat(chat);
        }
    }

    /**
     * Lifecycle callback to clean up relationships before entity removal
     */
    @PreRemove
    private void onPreRemove() {
        removeAllChats();
    }
}