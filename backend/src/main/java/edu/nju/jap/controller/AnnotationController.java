package edu.nju.jap.controller;

import edu.nju.jap.common.ApiResponse;
import edu.nju.jap.model.AnnotationSubmit;
import edu.nju.jap.service.AnnotationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/annotations")
public class AnnotationController {
    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @PostMapping("/submit")
    ApiResponse<Void> submit(@RequestBody AnnotationSubmit body, HttpServletRequest request) {
        annotationService.submit(body, request);
        return ApiResponse.ok("提交成功", null);
    }
}
