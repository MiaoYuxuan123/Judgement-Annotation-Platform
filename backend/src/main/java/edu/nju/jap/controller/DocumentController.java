package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.DocumentItem;
import edu.nju.jap.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(documentService.list(keyword));
    }

    @GetMapping("/{id}")
    ApiResponse<DocumentItem> detail(@PathVariable long id) {
        return ApiResponse.ok(documentService.getById(id));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) {
        documentService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        long id = documentService.create(body, request);
        return ApiResponse.ok("文书创建成功", Map.of("documentId", id));
    }

    @PostMapping("/upload")
    ApiResponse<Map<String, Object>> upload(@RequestParam("files") MultipartFile[] files) {
        return ApiResponse.ok("解析完成，请确认后保存", documentService.upload(files));
    }
}
