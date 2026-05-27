package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.dto.request.ArbitrationSubmit;
import edu.nju.jap.model.dto.response.TaskSummary;
import edu.nju.jap.model.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReviewService {
    private final DemoDataStore store;
    private final TaskService taskService;

    public ReviewService(DemoDataStore store, TaskService taskService) {
        this.store = store;
        this.taskService = taskService;
    }

    public Map<String, Object> review(long taskId) {
        TaskItem task = taskService.requireTask(taskId);
        List<Map<String, Object>> documents = task.documentIds.stream().map(docId -> {
            DocumentItem doc = store.documents.get(docId);
            List<AnnotationResult> results = task.annotatorIds.stream()
                    .map(uid -> store.annotations.get(DemoDataStore.annotationKey(taskId, docId, uid)))
                    .filter(Objects::nonNull)
                    .toList();
            ArbitrationResult finalResult = store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, docId));
            return Map.<String, Object>of("document", doc, "annotatorResults", results,
                    "finalResult", finalResult == null ? "" : finalResult);
        }).toList();
        return Map.of("task", taskService.toSummary(task), "documents", documents);
    }

    public void adopt(Map<String, Object> body, HttpServletRequest request) {
        long taskId = MapBodyUtils.longValue(body.get("taskId"), 0);
        long docId = MapBodyUtils.longValue(body.get("dataId"),
                MapBodyUtils.longValue(body.get("documentId"), 0));
        long annotatorId = MapBodyUtils.longValue(body.get("annotatorId"), 0);
        AnnotationResult source = store.annotations.get(DemoDataStore.annotationKey(taskId, docId, annotatorId));
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "标注结果不存在");
        }
        User user = store.current(request);
        store.arbitrations.put(DemoDataStore.arbitrationKey(taskId, docId),
                new ArbitrationResult(taskId, docId, user.id, source.propositions, source.relations,
                        String.valueOf(annotatorId), LocalDateTime.now()));
        taskService.requireTask(taskId).status = "可导出";
    }

    public void manual(ArbitrationSubmit body, HttpServletRequest request) {
        User user = store.current(request);
        ArbitrationResult result = new ArbitrationResult(body.taskId(), body.dataId(), user.id,
                body.propositions() == null ? List.of() : body.propositions(),
                body.relations() == null ? List.of() : body.relations(), "MANUAL", LocalDateTime.now());
        result.finalResult = false;
        store.arbitrations.put(DemoDataStore.arbitrationKey(body.taskId(), body.dataId()), result);
    }

    public void confirm(Map<String, Object> body, HttpServletRequest request) {
        long taskId = MapBodyUtils.longValue(body.get("taskId"), 0);
        long docId = MapBodyUtils.longValue(body.get("dataId"),
                MapBodyUtils.longValue(body.get("documentId"), 0));
        ArbitrationResult arb = store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, docId));
        if (arb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无待确认的裁定结果");
        }
        if (arb.finalResult) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该文书裁定结果已确认");
        }
        store.current(request);
        arb.finalResult = true;
        arb.arbitratedAt = LocalDateTime.now();
        taskService.requireTask(taskId).status = "可导出";
    }

    public void cancelPending(Map<String, Object> body) {
        long taskId = MapBodyUtils.longValue(body.get("taskId"), 0);
        long docId = MapBodyUtils.longValue(body.get("dataId"),
                MapBodyUtils.longValue(body.get("documentId"), 0));
        String key = DemoDataStore.arbitrationKey(taskId, docId);
        ArbitrationResult arb = store.arbitrations.get(key);
        if (arb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无待确认的裁定结果");
        }
        if (arb.finalResult) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已确认的裁定结果不可取消");
        }
        store.arbitrations.remove(key);
    }
}
