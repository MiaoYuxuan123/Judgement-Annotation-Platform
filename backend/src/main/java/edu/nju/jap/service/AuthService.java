package edu.nju.jap.service;

import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.dto.request.LoginRequest;
import edu.nju.jap.model.dto.response.LoginResponse;
import edu.nju.jap.model.dto.response.UserVO;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.SysUser;
import edu.nju.jap.service.support.JwtService;
import edu.nju.jap.service.support.DomainConverter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final SysUserMapper sysUserMapper;
    private final JwtService jwtService;

    public AuthService(SysUserMapper sysUserMapper, JwtService jwtService) {
        this.sysUserMapper = sysUserMapper;
        this.jwtService = jwtService;
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
        if (po.getStatus() == null || po.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已禁用");
        }
        if (po.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "用户数据异常：缺少 id");
        }
        User user = DomainConverter.toUser(po);
        String token = jwtService.issueToken(user);
        return new LoginResponse(token, UserVO.from(user));
    }
}
