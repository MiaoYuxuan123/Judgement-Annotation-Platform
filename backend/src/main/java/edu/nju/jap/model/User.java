package edu.nju.jap.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public Map<String, Object> safe() {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("username", username);
        m.put("realName", realName);
        m.put("role", role);
        m.put("canCreateTask", canCreateTask);
        m.put("status", status);
        m.put("lastSeen", lastSeen == null ? null : lastSeen.toString());
        return m;
    }
}
