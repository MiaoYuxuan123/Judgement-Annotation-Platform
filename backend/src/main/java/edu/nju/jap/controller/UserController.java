package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.ok(userService.me(request));
    }

    @GetMapping
    ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(userService.list());
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        long id = userService.create(body);
        return ApiResponse.ok("用户创建成功", Map.of("userId", id));
    }

    @PutMapping("/{id}")
    ApiResponse<Void> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        userService.update(id, body);
        return ApiResponse.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
