package com.example.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many Users ↔️ Many Chats (inverse side) */
    @ManyToMany(mappedBy = "chats")
    private Set<User> users = new HashSet<>();

    /** One Chat → Many ChatInfos (Messages) */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatInfo> chatInfos = new HashSet<>();

    public Chat() {}

    // Getters and setters

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Set<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
        user.getChats().add(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.getChats().remove(this);
    }

    public Set<ChatInfo> getChatInfos() {
        return chatInfos;
    }

    public void addChatInfo(ChatInfo chatInfo) {
        chatInfos.add(chatInfo);
        chatInfo.setChat(this);
    }

    public void removeChatInfo(ChatInfo chatInfo) {
        chatInfos.remove(chatInfo);
        chatInfo.setChat(null);
    }
}
