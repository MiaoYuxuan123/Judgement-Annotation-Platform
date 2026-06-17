package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    ApiResponse<Map<String, Object>> list(HttpServletRequest request) {
        return ApiResponse.ok(messageService.list(request));
    }

    @GetMapping("/unread-count")
    ApiResponse<Map<String, Object>> unreadCount(HttpServletRequest request) {
        return ApiResponse.ok(messageService.unreadCount(request));
    }

    @PutMapping("/{id}/read")
    ApiResponse<Void> markRead(@PathVariable long id, HttpServletRequest request) {
        messageService.markRead(id, request);
        return ApiResponse.ok(null, null);
    }

    @PutMapping("/read-all")
    ApiResponse<Void> markAllRead(HttpServletRequest request) {
        messageService.markAllRead(request);
        return ApiResponse.ok(null, null);
    }

    @DeleteMapping("/read")
    ApiResponse<Void> deleteRead(HttpServletRequest request) {
        messageService.deleteRead(request);
        return ApiResponse.ok(null, null);
    }
}
