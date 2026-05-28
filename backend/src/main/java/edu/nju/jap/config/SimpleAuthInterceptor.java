package edu.nju.jap.config;

import edu.nju.jap.model.entity.User;
import edu.nju.jap.service.support.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SimpleAuthInterceptor implements HandlerInterceptor {
    private final AuthTokenService authTokenService;

    public SimpleAuthInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        User user = authTokenService.resolveUser(request.getHeader("Authorization"));
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或 Token 无效");
        }
        request.setAttribute("currentUser", user);
        return true;
    }
}
