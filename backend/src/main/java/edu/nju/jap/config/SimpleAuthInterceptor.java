package edu.nju.jap.config;

import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {
    private final DemoDataStore store;

    public SimpleAuthInterceptor(DemoDataStore store) {
        this.store = store;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        User user = store.userFromHeader(request.getHeader("Authorization"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或 Token 无效");
        }
        request.setAttribute("currentUser", user);
        return true;
    }
}
