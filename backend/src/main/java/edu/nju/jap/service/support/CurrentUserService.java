package edu.nju.jap.service.support;

import edu.nju.jap.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {
    public User requireCurrent(HttpServletRequest request) {
        return (User) request.getAttribute("currentUser");
    }
}
