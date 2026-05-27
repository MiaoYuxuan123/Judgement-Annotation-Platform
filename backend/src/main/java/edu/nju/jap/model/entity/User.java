package edu.nju.jap.model.entity;

import java.time.LocalDateTime;

public class User {
    public long id;
    public String username;
    public String password;
    public String realName;
    public String role;
    public boolean canCreateTask;
    public String status;
    public LocalDateTime lastSeen;

    public User(long id, String username, String password, String realName, String role, boolean canCreateTask,
                String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
        this.canCreateTask = canCreateTask;
        this.status = status;
    }
}
