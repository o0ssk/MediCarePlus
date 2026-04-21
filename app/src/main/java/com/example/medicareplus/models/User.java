package com.example.medicareplus.models;
public class User {

    private String userId;
    private String name;
    private String email;
    private String role;
    private String linkedCaregiverId;

    public User() {
    }

    public User(String userId, String name, String email, String role, String linkedCaregiverId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.linkedCaregiverId = linkedCaregiverId;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getLinkedCaregiverId() { return linkedCaregiverId; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    public void setLinkedCaregiverId(String linkedCaregiverId) { this.linkedCaregiverId = linkedCaregiverId; }
}