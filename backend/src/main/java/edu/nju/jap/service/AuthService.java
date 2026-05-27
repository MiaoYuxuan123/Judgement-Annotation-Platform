package edu.nju.jap.service;

import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.LoginRequest;
import edu.nju.jap.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AuthService {
    private final DemoDataStore store;

    public AuthService(DemoDataStore store) {
        this.store = store;
    }

    public Map<String, Object> login(LoginRequest request) {
        User user = store.users.values().stream()
                .filter(u -> u.username.equals(request.username()) && u.password.equals(request.password()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        return Map.of("token", store.issueToken(user), "user", user.safe());
    }
}
