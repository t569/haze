package com.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;



@Entity
@Table(name="users")
public class User {
    

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;


    // Many Users to Many Chats
    @ManyToMany
    @JoinTable(
        name = "user_chats",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "chat_id")
    )

    private Set<Chat> chats = new HashSet<>();
    public User() { }

    public User(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Chat> getChats() {return chats;}
    public void addChat(Chat chat)
    {
        chats.add(chat);
        chat.getUsers().add(this);
    }
    public void removeChat(Chat chat)
    {
        chats.remove(chat);
        chat.getUsers().remove(this);
    }
}
