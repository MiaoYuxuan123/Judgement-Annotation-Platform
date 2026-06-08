package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.dto.request.ArbitrationSubmit;
import edu.nju.jap.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{taskId}")
    ApiResponse<Map<String, Object>> review(@PathVariable long taskId) {
        return ApiResponse.ok(reviewService.review(taskId));
    }

    @PostMapping("/adopt")
    ApiResponse<Void> adopt(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        reviewService.adopt(body, request);
        return ApiResponse.ok("裁定完成", null);
    }

    @PostMapping("/manual")
    ApiResponse<Void> manual(@RequestBody ArbitrationSubmit body, HttpServletRequest request) {
        reviewService.manual(body, request);
        return ApiResponse.ok("裁定草稿已保存，请在裁定界面确认", null);
    }

    @PostMapping("/confirm")
    ApiResponse<Void> confirm(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        reviewService.confirm(body, request);
        return ApiResponse.ok("裁定结果已确认", null);
    }

    @PostMapping("/cancel-pending")
    ApiResponse<Void> cancelPending(@RequestBody Map<String, Object> body) {
        reviewService.cancelPending(body);
        return ApiResponse.ok("已取消待确认的裁定结果", null);
    }

    @PostMapping("/reject")
    ApiResponse<Void> reject(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        reviewService.reject(body, request);
        return ApiResponse.ok("已退回标注员重新标注", null);
    }
}
