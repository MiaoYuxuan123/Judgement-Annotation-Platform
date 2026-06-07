package edu.nju.jap.service;

import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.dto.request.LoginRequest;
import edu.nju.jap.model.dto.response.LoginResponse;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.SysUser;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AuthService authService = new AuthService(sysUserMapper, jwtService, currentUserService);

    @Test
    void loginReturnsTokenAndUserInfo() {
        SysUser user = userPo(3L, "annotator1", "123456");
        when(sysUserMapper.selectByUsername("annotator1")).thenReturn(user);
        when(jwtService.issueToken(any(User.class))).thenReturn("test-token");

        LoginResponse response = authService.login(new LoginRequest("annotator1", "123456"));

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.user().username()).isEqualTo("annotator1");
        verify(sysUserMapper).updateOnline(3L);
    }

    @Test
    void loginRejectsUnknownUser() {
        when(sysUserMapper.selectByUsername("missing")).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing", "123456")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getReason()).isEqualTo("用户名或密码错误");
                });
    }

    @Test
    void loginRejectsWrongPassword() {
        when(sysUserMapper.selectByUsername("annotator1")).thenReturn(userPo(3L, "annotator1", "123456"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("annotator1", "bad")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getReason()).isEqualTo("用户名或密码错误");
                });
        verify(sysUserMapper, never()).updateOnline(anyLong());
    }

    private static SysUser userPo(long id, String username, String password) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setRealName(username);
        user.setRole("USER");
        user.setCanCreateTask(0);
        user.setStatus(0);
        return user;
    }
}
