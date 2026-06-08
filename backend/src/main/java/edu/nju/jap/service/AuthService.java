package edu.nju.jap.service;

import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.dto.request.LoginRequest;
import edu.nju.jap.model.dto.response.LoginResponse;
import edu.nju.jap.model.dto.response.UserVO;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.SysUser;
import edu.nju.jap.service.support.JwtService;
import edu.nju.jap.service.support.DomainConverter;
import edu.nju.jap.service.support.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final SysUserMapper sysUserMapper;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(SysUserMapper sysUserMapper, JwtService jwtService, CurrentUserService currentUserService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser po = sysUserMapper.selectByUsername(request.username());
        if (po == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String stored = po.getPasswordHash();
        if (stored == null || !stored.equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (po.getIsDeleted() != null && po.getIsDeleted() == 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "该账号已被注销");
        }
        if (po.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "用户数据异常：缺少 id");
        }
        User user = DomainConverter.toUser(po);
        sysUserMapper.updateOnline(po.getId());
        String token = jwtService.issueToken(user);
        return new LoginResponse(token, UserVO.from(user));
    }

    public void logout(HttpServletRequest request) {
        User user = currentUserService.requireCurrent(request);
        if (user != null) {
            sysUserMapper.updateOffline(user.id);
        }
    }
}
