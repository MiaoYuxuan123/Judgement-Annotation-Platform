package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.dto.response.TaskDetail;
import edu.nju.jap.model.entity.ArbitrationResult;
import edu.nju.jap.service.TaskService;
import edu.nju.jap.service.TaskDocumentUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskDocumentUploadService taskDocumentUploadService;

    public TaskController(TaskService taskService, TaskDocumentUploadService taskDocumentUploadService) {
        this.taskService = taskService;
        this.taskDocumentUploadService = taskDocumentUploadService;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(taskService.list(status, keyword));
    }

    @GetMapping("/my")
    ApiResponse<Map<String, Object>> myTasks(HttpServletRequest request) {
        return ApiResponse.ok(taskService.myTasks(request));
    }

    @PostMapping
    ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        long id = taskService.create(body, request);
        return ApiResponse.ok("任务创建成功", Map.of("taskId", id));
    }

    @PostMapping("/documents/upload")
    ApiResponse<Map<String, Object>> uploadDocuments(@RequestParam("files") MultipartFile[] files) {
        return ApiResponse.ok("解析完成", taskDocumentUploadService.upload(files));
    }

    @GetMapping("/{id}")
    ApiResponse<TaskDetail> detail(@PathVariable long id) {
        return ApiResponse.ok(taskService.detail(id));
    }

    @PutMapping("/{id}/stage")
    ApiResponse<TaskDetail> advance(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return ApiResponse.ok("阶段已推进", taskService.advance(id, body));
    }

    @PutMapping("/{id}/config")
    ApiResponse<TaskDetail> updateConfig(@PathVariable long id, @RequestBody Map<String, Object> body,
                                         HttpServletRequest request) {
        return ApiResponse.ok("配置已更新", taskService.updateConfig(id, body, request));
    }

    @GetMapping("/{taskId}/items")
    ApiResponse<List<Map<String, Object>>> items(@PathVariable long taskId) {
        return ApiResponse.ok(taskService.items(taskId));
    }

    @GetMapping("/{taskId}/items/{dataId}")
    ApiResponse<Map<String, Object>> item(@PathVariable long taskId, @PathVariable long dataId,
                                          @RequestParam(required = false) Long sourceUserId,
                                          @RequestParam(required = false) Boolean sourceArbitration,
                                          HttpServletRequest request) {
        return ApiResponse.ok(taskService.item(taskId, dataId, sourceUserId, sourceArbitration, request));
    }

    @GetMapping("/{taskId}/results")
    ApiResponse<List<ArbitrationResult>> results(@PathVariable long taskId) {
        return ApiResponse.ok(taskService.results(taskId));
    }

    @GetMapping("/{taskId}/export")
    void export(@PathVariable long taskId) {
        taskService.export(taskId);
    }
}
