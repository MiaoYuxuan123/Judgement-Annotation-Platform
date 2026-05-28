package edu.nju.jap.service.support;

import edu.nju.jap.mapper.AuthTokenMapper;
import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.entity.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthTokenService {
    private final AuthTokenMapper authTokenMapper;
    private final SysUserMapper sysUserMapper;

    public AuthTokenService(AuthTokenMapper authTokenMapper, SysUserMapper sysUserMapper) {
        this.authTokenMapper = authTokenMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public String issueToken(User user) {
        String token = "demo-token-" + user.id + "-" + UUID.randomUUID();
        authTokenMapper.insert(token, user.id);
        return token;
    }

    public User resolveUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        Long userId = authTokenMapper.selectUserId(token);
        if (userId == null) {
            return null;
        }
        return DomainConverter.toUser(sysUserMapper.selectById(userId));
    }
}
