package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final DemoDataStore store;

    public UserService(DemoDataStore store) {
        this.store = store;
    }

    public Map<String, Object> me(HttpServletRequest request) {
        return store.current(request).safe();
    }

    public List<Map<String, Object>> list() {
        return store.users.values().stream().map(User::safe).toList();
    }

    public long create(Map<String, Object> body) {
        long id = store.userSeq.incrementAndGet();
        User user = new User(id, MapBodyUtils.text(body, "username", "user" + id),
                MapBodyUtils.text(body, "password", "123456"),
                MapBodyUtils.text(body, "realName", "新用户" + id), MapBodyUtils.text(body, "role", "annotator"),
                MapBodyUtils.bool(body, "canCreateTask", false), "正常");
        store.users.put(id, user);
        return id;
    }

    public void update(long id, Map<String, Object> body) {
        User old = requireUser(id);
        store.users.put(id, new User(id, old.username, MapBodyUtils.text(body, "password", old.password),
                MapBodyUtils.text(body, "realName", old.realName), MapBodyUtils.text(body, "role", old.role),
                MapBodyUtils.bool(body, "canCreateTask", old.canCreateTask),
                MapBodyUtils.text(body, "status", old.status)));
    }

    public void delete(long id) {
        store.users.remove(id);
    }

    public User requireUser(long id) {
        User user = store.users.get(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
