package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.dto.response.UserVO;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.SysUser;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.DomainConverter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final SysUserMapper sysUserMapper;
    private final CurrentUserService currentUserService;

    public UserService(SysUserMapper sysUserMapper, CurrentUserService currentUserService) {
        this.sysUserMapper = sysUserMapper;
        this.currentUserService = currentUserService;
    }

    public UserVO me(HttpServletRequest request) {
        return UserVO.from(currentUserService.requireCurrent(request));
    }

    public List<UserVO> list() {
        return sysUserMapper.selectAll().stream()
                .filter(po -> po.getIsDeleted() == null || po.getIsDeleted() != 1)
                .map(po -> UserVO.from(DomainConverter.toUser(po)))
                .toList();
    }

    public long create(Map<String, Object> body) {
        String username = MapBodyUtils.text(body, "username", "user" + System.currentTimeMillis());
        SysUser existing = sysUserMapper.selectByUsername(username);
        if (existing != null) {
            if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
                existing.setPasswordHash(MapBodyUtils.text(body, "password", existing.getPasswordHash()));
                existing.setRealName(MapBodyUtils.text(body, "realName", existing.getRealName()));
                existing.setRole(MapBodyUtils.text(body, "role", existing.getRole()));
                existing.setCanCreateTask("creator".equals(existing.getRole()) ? 1 : 0);
                existing.setStatus(1);
                existing.setIsDeleted(0);
                sysUserMapper.update(existing);
                return existing.getId();
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(MapBodyUtils.text(body, "password", "123456"));
        user.setRealName(MapBodyUtils.text(body, "realName", "新用户"));
        user.setRole(MapBodyUtils.text(body, "role", "user"));
        user.setCanCreateTask("creator".equals(user.getRole()) ? 1 : 0);
        user.setStatus(1);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);
        return user.getId();
    }

    public void update(long id, Map<String, Object> body) {
        SysUser old = requirePo(id);
        old.setUsername(MapBodyUtils.text(body, "username", old.getUsername()));
        old.setPasswordHash(MapBodyUtils.text(body, "password", old.getPasswordHash()));
        old.setRealName(MapBodyUtils.text(body, "realName", old.getRealName()));
        old.setRole(MapBodyUtils.text(body, "role", old.getRole()));
        old.setCanCreateTask("creator".equals(old.getRole()) ? 1 : 0);
        String statusText = MapBodyUtils.text(body, "status", old.getStatus() == 1 ? "在线" : "离线");
        old.setStatus("在线".equals(statusText) ? 1 : 0);
        sysUserMapper.update(old);
    }

    public void delete(long id) {
        sysUserMapper.deleteById(id);
    }

    public User requireUser(long id) {
        return DomainConverter.toUser(requirePo(id));
    }

    private SysUser requirePo(long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public List<Map<String, Object>> batchCreate(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少提供一个用户");
        }
        String defaultRole = MapBodyUtils.text(body, "role", "user");
        String defaultPassword = MapBodyUtils.text(body, "password", "123456");
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> entry : users) {
            String username = MapBodyUtils.text(entry, "username", "").trim();
            String realName = MapBodyUtils.text(entry, "realName", "").trim();
            if (username.isEmpty()) continue;
            SysUser existing = sysUserMapper.selectByUsername(username);
            if (existing != null) {
                if (existing.getIsDeleted() != null && existing.getIsDeleted() == 1) {
                    existing.setPasswordHash(defaultPassword);
                    existing.setRealName(realName.isEmpty() ? username : realName);
                    existing.setRole(defaultRole);
                    existing.setCanCreateTask("creator".equals(defaultRole) ? 1 : 0);
                    existing.setStatus(1);
                    existing.setIsDeleted(0);
                    sysUserMapper.update(existing);
                    results.add(Map.of("id", existing.getId(), "username", username, "realName", realName));
                }
                continue;
            }
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPasswordHash(defaultPassword);
            user.setRealName(realName.isEmpty() ? username : realName);
            user.setRole(defaultRole);
            user.setCanCreateTask("creator".equals(defaultRole) ? 1 : 0);
            user.setStatus(1);
            user.setIsDeleted(0);
            sysUserMapper.insert(user);
            results.add(Map.of("id", user.getId(), "username", username, "realName", realName));
        }
        return results;
    }
}
