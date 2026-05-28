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
        return sysUserMapper.selectAll().stream().map(po -> UserVO.from(DomainConverter.toUser(po))).toList();
    }

    public long create(Map<String, Object> body) {
        SysUser user = new SysUser();
        user.setUsername(MapBodyUtils.text(body, "username", "user" + System.currentTimeMillis()));
        user.setPasswordHash(MapBodyUtils.text(body, "password", "123456"));
        user.setRealName(MapBodyUtils.text(body, "realName", "新用户"));
        user.setRole(MapBodyUtils.text(body, "role", "user"));
        user.setCanCreateTask(MapBodyUtils.bool(body, "canCreateTask", false) ? 1 : 0);
        user.setStatus(1);
        sysUserMapper.insert(user);
        return user.getId();
    }

    public void update(long id, Map<String, Object> body) {
        SysUser old = requirePo(id);
        old.setPasswordHash(MapBodyUtils.text(body, "password", old.getPasswordHash()));
        old.setRealName(MapBodyUtils.text(body, "realName", old.getRealName()));
        old.setRole(MapBodyUtils.text(body, "role", old.getRole()));
        old.setCanCreateTask(MapBodyUtils.bool(body, "canCreateTask", old.getCanCreateTask() == 1) ? 1 : 0);
        String statusText = MapBodyUtils.text(body, "status", old.getStatus() == 1 ? "正常" : "禁用");
        old.setStatus("正常".equals(statusText) ? 1 : 0);
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
}
