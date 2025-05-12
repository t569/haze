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

    /** One Chat → One ChatInfo */
    @OneToOne(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "chat_info_id", referencedColumnName = "id")
    private ChatInfo chatInfo;

    public Chat() { }

    // getters & setters

    public Long getId() { return id; }

    public Set<User> getUsers() { return users; }
    public void addUser(User user) {
        users.add(user);
        user.getChats().add(this);
    }
    public void removeUser(User user) {
        users.remove(user);
        user.getChats().remove(this);
    }

    public ChatInfo getChatInfo() { return chatInfo; }
    public void setChatInfo(ChatInfo chatInfo) {
        this.chatInfo = chatInfo;
        chatInfo.setChat(this);
    }
}
