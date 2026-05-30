package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.GlobalDocumentMapper;
import edu.nju.jap.mapper.TaskDocumentMapper;
import edu.nju.jap.mapper.TaskMapper;
import edu.nju.jap.mapper.TaskMemberMapper;
import edu.nju.jap.model.dto.response.TaskDetail;
import edu.nju.jap.model.dto.response.TaskSummary;
import edu.nju.jap.model.entity.AnnotationResult;
import edu.nju.jap.model.entity.ArbitrationResult;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.Task;
import edu.nju.jap.model.po.TaskDocument;
import edu.nju.jap.model.po.TaskMember;
import edu.nju.jap.service.support.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TaskService {
    private final TaskMapper taskMapper;
    private final TaskMemberMapper taskMemberMapper;
    private final TaskDocumentMapper taskDocumentMapper;
    private final GlobalDocumentMapper globalDocumentMapper;
    private final TaskAggregateService taskAggregateService;
    private final TaskDocumentResolver taskDocumentResolver;
    private final CurrentUserService currentUserService;
    private final AnnotationPersistenceService annotationPersistenceService;
    private final TaskDocumentFactory taskDocumentFactory;
    private final edu.nju.jap.mapper.ArbitrationSnapshotMapper arbitrationSnapshotMapper;

    public TaskService(TaskMapper taskMapper, TaskMemberMapper taskMemberMapper, TaskDocumentMapper taskDocumentMapper,
                       GlobalDocumentMapper globalDocumentMapper, TaskAggregateService taskAggregateService,
                       TaskDocumentResolver taskDocumentResolver, CurrentUserService currentUserService,
                       AnnotationPersistenceService annotationPersistenceService,
                       TaskDocumentFactory taskDocumentFactory,
                       edu.nju.jap.mapper.ArbitrationSnapshotMapper arbitrationSnapshotMapper) {
        this.taskMapper = taskMapper;
        this.taskMemberMapper = taskMemberMapper;
        this.taskDocumentMapper = taskDocumentMapper;
        this.globalDocumentMapper = globalDocumentMapper;
        this.taskAggregateService = taskAggregateService;
        this.taskDocumentResolver = taskDocumentResolver;
        this.currentUserService = currentUserService;
        this.annotationPersistenceService = annotationPersistenceService;
        this.taskDocumentFactory = taskDocumentFactory;
        this.arbitrationSnapshotMapper = arbitrationSnapshotMapper;
    }

    public Map<String, Object> list(String status, String keyword) {
        List<TaskSummary> list = taskMapper.selectAll(status, keyword).stream()
                .map(t -> taskAggregateService.toSummary(taskAggregateService.loadTaskItem(t.getId())))
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    public Map<String, Object> myTasks(HttpServletRequest request) {
        long userId = currentUserService.requireCurrent(request).id;
        List<TaskSummary> list = taskMapper.selectByUserId(userId).stream()
                .map(t -> taskAggregateService.toSummary(taskAggregateService.loadTaskItem(t.getId())))
                .toList();
        return Map.of("total", list.size(), "list", list);
    }

    @Transactional
    public long create(Map<String, Object> body, HttpServletRequest request) {
        User user = currentUserService.requireCurrent(request);
        List<Map<String, Object>> documents = MapBodyUtils.mapList(body.get("documents"));
        if (documents.isEmpty()) {
            documents = legacyGlobalDocuments(MapBodyUtils.longList(body.get("documentIds")));
        }
        if (documents.isEmpty()) {
            documents = legacyGlobalDocuments(List.of(101L));
        }
        List<Long> annotators = MapBodyUtils.longList(body.get("annotatorIds"));
        if (annotators.isEmpty()) {
            annotators = List.of(3L, 4L);
        }
        long reviewerId = MapBodyUtils.longValue(body.get("reviewerId"), 5);
        int configId = (int) MapBodyUtils.longValue(body.get("configId"), 1);

        Task task = new Task();
        task.setTitle(MapBodyUtils.text(body, "taskName", "新建标注任务"));
        task.setDescription(MapBodyUtils.text(body, "description", "课程演示任务"));
        task.setStatus("标注中");
        task.setCreatorId(user.id);
        task.setGuideVersionId(configId);
        taskMapper.insert(task);

        for (Long uid : annotators) {
            TaskMember m = new TaskMember();
            m.setTaskId(task.getId());
            m.setUserId(uid);
            m.setRoleInTask("标注员");
            taskMemberMapper.insert(m);
        }
        TaskMember reviewer = new TaskMember();
        reviewer.setTaskId(task.getId());
        reviewer.setUserId(reviewerId);
        reviewer.setRoleInTask("裁定者");
        taskMemberMapper.insert(reviewer);

        for (Map<String, Object> spec : documents) {
            TaskDocument td = taskDocumentFactory.buildForCreate(task.getId(), spec, user.id);
            taskDocumentMapper.insert(td);
        }
        return task.getId();
    }

    private List<Map<String, Object>> legacyGlobalDocuments(List<Long> documentIds) {
        return documentIds.stream()
                .map(id -> Map.<String, Object>of("sourceType", "GLOBAL", "globalDocId", id))
                .toList();
    }

    public TaskDetail detail(long id) {
        TaskItem task = requireTask(id);
        return new TaskDetail(taskAggregateService.toSummary(task),
                taskAggregateService.listTaskDocuments((int) task.id).stream()
                        .map(taskDocumentResolver::toDocumentItem).toList(),
                taskAggregateService.annotatorVos(task),
                taskAggregateService.reviewerVo(task),
                task.configSnapshot);
    }

    public TaskDetail advance(long id, Map<String, Object> body) {
        TaskItem task = requireTask(id);
        String target = MapBodyUtils.text(body, "status", nextStatus(task.status));
        List<String> order = List.of("标注中", "待裁定", "可导出");
        if (order.indexOf(target) < order.indexOf(task.status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "任务阶段不可回退");
        }
        taskMapper.updateStatus((int) id, target);
        return detail(id);
    }

    public List<Map<String, Object>> items(long taskId) {
        int tid = (int) taskId;
        TaskItem task = requireTask(taskId);
        return taskAggregateService.listTaskDocuments(tid).stream().map(td -> {
            long dataId = taskDocumentResolver.apiDataId(td);
            edu.nju.jap.model.po.GlobalDocument global = td.getGlobalDocId() == null ? null
                    : globalDocumentMapper.selectById(td.getGlobalDocId());
            String docCode = global != null ? String.valueOf(global.getId()) : ("D" + td.getId());
            String title = global != null ? global.getTitle() : td.getFileName();
            return Map.<String, Object>of("dataId", dataId, "documentId", docCode, "title", title, "status", task.status);
        }).toList();
    }

    public Map<String, Object> item(long taskId, long dataId, Long sourceUserId, Boolean sourceArbitration,
                                    HttpServletRequest request) {
        int tid = (int) taskId;
        TaskItem task = requireTask(taskId);
        TaskDocument td = taskDocumentResolver.requireTaskDocument(tid, dataId);
        int taskDocId = td.getId();
        long apiDataId = taskDocumentResolver.apiDataId(td);
        User user = currentUserService.requireCurrent(request);
        AnnotationResult annotation;
        if (sourceUserId != null) {
            annotation = annotationPersistenceService.loadAnnotation(tid, taskDocId, sourceUserId, apiDataId);
        } else if (Boolean.TRUE.equals(sourceArbitration)) {
            var snapshot = arbitrationSnapshotMapper.selectByTaskAndDoc(tid, taskDocId);
            if (snapshot == null) {
                annotation = new AnnotationResult(taskId, apiDataId, user.id, List.of(), List.of(), true, null);
            } else {
                ArbitrationResult arb = annotationPersistenceService.loadArbitration(tid, taskDocId, apiDataId,
                        snapshot.getArbitratorId(), snapshot);
                annotation = new AnnotationResult(taskId, apiDataId, user.id, arb.propositions, arb.relations,
                        false, arb.arbitratedAt);
            }
        } else {
            annotation = annotationPersistenceService.loadAnnotation(tid, taskDocId, user.id, apiDataId);
        }
        return Map.of("task", taskAggregateService.toSummary(task), "document", taskDocumentResolver.toDocumentItem(td),
                "config", task.configSnapshot, "annotation", annotation);
    }

    public List<ArbitrationResult> results(long taskId) {
        int tid = (int) taskId;
        requireTask(taskId);
        return arbitrationSnapshotMapper.selectByTaskId(tid).stream()
                .map(s -> {
                    TaskDocument td = taskDocumentMapper.selectById(s.getTaskDocumentId());
                    long apiDataId = taskDocumentResolver.apiDataId(td);
                    return annotationPersistenceService.loadArbitration(tid, s.getTaskDocumentId(), apiDataId,
                            s.getArbitratorId(), s);
                })
                .toList();
    }

    public void export(long taskId) {
        requireTask(taskId);
        throw new ResponseStatusException(HttpStatus.GONE,
                "导出已改为浏览器端 ZIP 打包，请在「结果查看 / 导出」页面点击「导出 ZIP」并选择保存位置");
    }

    public TaskItem requireTask(long id) {
        return taskAggregateService.loadTaskItem((int) id);
    }

    public TaskSummary toSummary(TaskItem task) {
        return taskAggregateService.toSummary(task);
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
