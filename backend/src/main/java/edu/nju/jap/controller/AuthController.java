package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.dto.request.LoginRequest;
import edu.nju.jap.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import edu.nju.jap.model.dto.response.LoginResponse;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.ok("已退出", null);
    }
}
