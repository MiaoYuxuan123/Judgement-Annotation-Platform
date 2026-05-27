package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.dao.DemoDataStore;
import edu.nju.jap.model.dto.response.TaskDetail;
import edu.nju.jap.model.dto.response.TaskSummary;
import edu.nju.jap.model.dto.response.UserVO;
import edu.nju.jap.model.entity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskService {
    private final DemoDataStore store;
    private final DocumentService documentService;

    public TaskService(DemoDataStore store, DocumentService documentService) {
        this.store = store;
        this.documentService = documentService;
    }

    public Map<String, Object> list(String status, String keyword) {
        List<TaskSummary> list = store.tasks.values().stream()
                .filter(t -> status == null || status.isBlank() || t.status.equals(status))
                .filter(t -> keyword == null || keyword.isBlank() || t.taskName.contains(keyword))
                .sorted(Comparator.comparing((TaskItem t) -> t.id).reversed())
                .map(this::toSummary)
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    public Map<String, Object> myTasks(HttpServletRequest request) {
        User user = store.current(request);
        List<TaskSummary> list = store.tasks.values().stream()
                .filter(t -> t.creatorId == user.id || t.annotatorIds.contains(user.id) || t.reviewerId == user.id)
                .sorted(Comparator.comparing((TaskItem t) -> t.id).reversed())
                .map(this::toSummary)
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    public long create(Map<String, Object> body, HttpServletRequest request) {
        User user = store.current(request);
        List<Long> documentIds = MapBodyUtils.longList(body.get("documentIds"));
        if (documentIds.isEmpty()) {
            documentIds = List.of(101L);
        }
        List<Long> annotators = MapBodyUtils.longList(body.get("annotatorIds"));
        if (annotators.isEmpty()) {
            annotators = List.of(3L, 4L);
        }
        long reviewerId = MapBodyUtils.longValue(body.get("reviewerId"), 5);
        long configId = MapBodyUtils.longValue(body.get("configId"), 1);
        long id = store.taskSeq.incrementAndGet();
        TaskItem task = new TaskItem(id, MapBodyUtils.text(body, "taskName", "新建标注任务" + id),
                MapBodyUtils.text(body, "description", "课程演示任务"), "标注中", configId, documentIds,
                annotators, reviewerId, user.id, LocalDateTime.now(), store.configs.get(configId));
        store.tasks.put(id, task);
        return id;
    }

    public TaskDetail detail(long id) {
        return toDetail(requireTask(id));
    }

    public TaskDetail advance(long id, Map<String, Object> body) {
        TaskItem task = requireTask(id);
        String target = MapBodyUtils.text(body, "status", nextStatus(task.status));
        List<String> order = List.of("标注中", "待裁定", "可导出");
        if (order.indexOf(target) < order.indexOf(task.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "任务阶段不可回退");
        }
        task.status = target;
        task.stageChangedAt = LocalDateTime.now();
        return toDetail(task);
    }

    public List<Map<String, Object>> items(long taskId) {
        TaskItem task = requireTask(taskId);
        return task.documentIds.stream().map(docId -> {
            DocumentItem doc = store.documents.get(docId);
            return Map.<String, Object>of("dataId", docId, "documentId", doc.documentId, "title", doc.title,
                    "status", task.status);
        }).toList();
    }

    public Map<String, Object> item(long taskId, long dataId, Long sourceUserId, Boolean sourceArbitration,
                                    HttpServletRequest request) {
        TaskItem task = requireTask(taskId);
        DocumentItem doc = documentService.getById(dataId);
        User user = store.current(request);
        AnnotationResult annotation;
        if (sourceUserId != null) {
            annotation = store.annotations.get(DemoDataStore.annotationKey(taskId, dataId, sourceUserId));
            if (annotation == null) {
                annotation = new AnnotationResult(taskId, dataId, sourceUserId, List.of(), List.of(), false, null);
            }
        } else if (Boolean.TRUE.equals(sourceArbitration)) {
            ArbitrationResult arb = store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, dataId));
            if (arb == null) {
                annotation = new AnnotationResult(taskId, dataId, user.id, List.of(), List.of(), true, null);
            } else {
                annotation = new AnnotationResult(taskId, dataId, user.id, arb.propositions, arb.relations, false,
                        arb.arbitratedAt);
            }
        } else {
            annotation = store.annotations.get(DemoDataStore.annotationKey(taskId, dataId, user.id));
            if (annotation == null) {
                annotation = new AnnotationResult(taskId, dataId, user.id, List.of(), List.of(), true, null);
            }
        }
        return Map.of("task", toSummary(task), "document", doc, "config", task.configSnapshot, "annotation",
                annotation);
    }

    public List<ArbitrationResult> results(long taskId) {
        TaskItem task = requireTask(taskId);
        return task.documentIds.stream()
                .map(docId -> store.arbitrations.get(DemoDataStore.arbitrationKey(taskId, docId)))
                .filter(Objects::nonNull)
                .toList();
    }

    public void export(long taskId) {
        requireTask(taskId);
        throw new ResponseStatusException(HttpStatus.GONE,
                "导出已改为浏览器端 ZIP 打包，请在「结果查看 / 导出」页面点击「导出 ZIP」并选择保存位置");
    }

    public TaskItem requireTask(long id) {
        TaskItem task = store.tasks.get(id);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    public TaskSummary toSummary(TaskItem task) {
        User reviewer = store.users.get(task.reviewerId);
        User creator = store.users.get(task.creatorId);
        return new TaskSummary(task.id, task.taskName, task.description, task.status, task.documentIds.size(),
                task.annotatorIds.size(), reviewer == null ? "-" : reviewer.realName,
                creator == null ? "-" : creator.realName, task.createdAt);
    }

    public TaskDetail toDetail(TaskItem task) {
        return new TaskDetail(toSummary(task),
                task.documentIds.stream().map(store.documents::get).filter(Objects::nonNull).toList(),
                task.annotatorIds.stream().map(id -> UserVO.from(store.users.get(id))).toList(),
                UserVO.from(store.users.get(task.reviewerId)),
                task.configSnapshot);
    }

    public static String nextStatus(String status) {
        if ("标注中".equals(status)) {
            return "待裁定";
        }
        if ("待裁定".equals(status)) {
            return "可导出";
        }
        return "可导出";
    }
}
