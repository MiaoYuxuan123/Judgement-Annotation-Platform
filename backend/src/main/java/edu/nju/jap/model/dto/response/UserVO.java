package edu.nju.jap.model.dto.response;

import edu.nju.jap.model.entity.User;

public record UserVO(long id, String username, String realName, String role, boolean canCreateTask, String status,
                     String lastSeen) {
    public static UserVO from(User user) {
        return new UserVO(user.id, user.username, user.realName, user.role, user.canCreateTask, user.status,
                user.lastSeen == null ? null : user.lastSeen.toString());
    }
}
