package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.service.ConfigService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/{id}")
    ApiResponse<GuideConfig> get(@PathVariable long id) {
        return ApiResponse.ok(configService.requireConfig(id));
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

    @PostMapping("/{id}/attachment")
    ApiResponse<Void> uploadAttachment(@PathVariable int id, @RequestParam("file") MultipartFile file) throws IOException {
        configService.uploadAttachment(id, file);
        return ApiResponse.ok("附件上传成功", null);
    }

    @GetMapping("/{id}/attachment")
    ResponseEntity<Resource> downloadAttachment(@PathVariable int id) {
        java.io.File file = configService.getAttachmentFile(id);
        Resource resource = new FileSystemResource(file);
        GuideConfig config = configService.requireConfig(id);
        String filename = config.attachmentName != null ? config.attachmentName : "attachment";
        String contentType = filename.toLowerCase().endsWith(".pdf") ? "application/pdf"
                : filename.toLowerCase().endsWith(".txt") ? "text/plain"
                : filename.toLowerCase().endsWith(".docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(resource);
    }
}
