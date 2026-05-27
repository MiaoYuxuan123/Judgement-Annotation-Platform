package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.GuideConfig;
import edu.nju.jap.service.ConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configs/versions")
public class ConfigController {
    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    ApiResponse<List<GuideConfig>> list() {
        return ApiResponse.ok(configService.list());
    }

    @GetMapping("/active")
    ApiResponse<GuideConfig> active() {
        return ApiResponse.ok(configService.active());
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        long id = configService.create(body);
        return ApiResponse.ok("配置保存成功", Map.of("configId", id));
    }

    @PutMapping("/{id}")
    ApiResponse<GuideConfig> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("配置更新成功", configService.update(id, body));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) {
        configService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }
}
