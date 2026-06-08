package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.AnnotationMapper;
import edu.nju.jap.mapper.ArbitrationSnapshotMapper;
import edu.nju.jap.model.dto.request.ArbitrationSubmit;
import edu.nju.jap.model.entity.AnnotationResult;
import edu.nju.jap.model.entity.ArbitrationResult;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.ArbitrationSnapshot;
import edu.nju.jap.model.po.TaskDocument;
import edu.nju.jap.service.support.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {
    private final TaskService taskService;
    private final TaskAggregateService taskAggregateService;
    private final TaskDocumentResolver taskDocumentResolver;
    private final AnnotationPersistenceService annotationPersistenceService;
    private final AnnotationMapper annotationMapper;
    private final ArbitrationSnapshotMapper arbitrationSnapshotMapper;
    private final CurrentUserService currentUserService;
    private final TaskStageSyncService taskStageSyncService;

    public ReviewService(TaskService taskService, TaskAggregateService taskAggregateService,
                         TaskDocumentResolver taskDocumentResolver,
                         AnnotationPersistenceService annotationPersistenceService,
                         AnnotationMapper annotationMapper,
                         ArbitrationSnapshotMapper arbitrationSnapshotMapper,
                         CurrentUserService currentUserService, TaskStageSyncService taskStageSyncService) {
        this.taskService = taskService;
        this.taskAggregateService = taskAggregateService;
        this.taskDocumentResolver = taskDocumentResolver;
        this.annotationPersistenceService = annotationPersistenceService;
        this.annotationMapper = annotationMapper;
        this.arbitrationSnapshotMapper = arbitrationSnapshotMapper;
        this.currentUserService = currentUserService;
        this.taskStageSyncService = taskStageSyncService;
    }

    public Map<String, Object> review(long taskId, HttpServletRequest request) {
        TaskItem task = taskService.requireTask(taskId);
        User user = currentUserService.requireCurrent(request);
        boolean canSeeAll = canSeeAllReviewResults(task, user);
        int tid = (int) taskId;
        int annotatorCount = task.annotatorIds.size();
        List<Map<String, Object>> documents = taskAggregateService.listTaskDocuments(tid).stream().map(td -> {
            long dataId = taskDocumentResolver.apiDataId(td);
            List<AnnotationResult> results = task.annotatorIds.stream()
                    .map(uid -> annotationPersistenceService.loadAnnotation(tid, td.getId(), uid, dataId))
                    .filter(a -> !a.propositions.isEmpty() || !a.relations.isEmpty())
                    .filter(a -> canSeeAll || a.userId == user.id)
                    .toList();
            ArbitrationSnapshot snap = arbitrationSnapshotMapper.selectByTaskAndDoc(tid, td.getId());
            Object finalResult = resolveFinalResultForViewer(tid, td.getId(), dataId, snap, canSeeAll);
            int submittedCount = annotationMapper.countSubmittedByTaskDocument(tid, td.getId());
            boolean allSubmitted = annotatorCount > 0 && submittedCount >= annotatorCount;
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("document", taskDocumentResolver.toDocumentItem(td));
            doc.put("annotatorResults", results);
            doc.put("finalResult", finalResult);
            doc.put("annotatorCount", annotatorCount);
            doc.put("submittedAnnotatorCount", submittedCount);
            doc.put("allAnnotatorsSubmitted", allSubmitted);
            return doc;
        }).toList();
        return Map.of("task", taskService.toSummary(task), "documents", documents);
    }

    /** 仅本任务的创建者与裁定者可查看全部标注员结果及裁定草稿。 */
    private boolean canSeeAllReviewResults(TaskItem task, User user) {
        return user.id == task.reviewerId || user.id == task.creatorId;
    }

    private Object resolveFinalResultForViewer(int taskId, int taskDocumentId, long dataId, ArbitrationSnapshot snap,
                                               boolean canSeeAll) {
        if (snap == null) {
            return "";
        }
        if (!canSeeAll && (snap.getFinalResult() == null || snap.getFinalResult() != 1)) {
            return "";
        }
        return annotationPersistenceService.loadArbitration(taskId, taskDocumentId, dataId, snap.getArbitratorId(),
                snap);
    }

    @Transactional
    public void adopt(Map<String, Object> body, HttpServletRequest request) {
        int taskId = (int) MapBodyUtils.longValue(body.get("taskId"), 0);
        long dataId = MapBodyUtils.longValue(body.get("dataId"), MapBodyUtils.longValue(body.get("documentId"), 0));
        long annotatorId = MapBodyUtils.longValue(body.get("annotatorId"), 0);
        TaskDocument td = taskDocumentResolver.requireTaskDocument(taskId, dataId);
        requireAllAnnotatorsSubmitted(taskId, td);
        AnnotationResult source = annotationPersistenceService.loadAnnotation(taskId, td.getId(), annotatorId,
                taskDocumentResolver.apiDataId(td));
        if (source.propositions.isEmpty() && source.relations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "标注结果不存在");
        }
        long arbitratorId = currentUserService.requireCurrent(request).id;
        annotationPersistenceService.saveArbitration(taskId, td.getId(), arbitratorId, source.propositions,
                source.relations, false, source.graphLayout);
        upsertSnapshot(taskId, td.getId(), arbitratorId, String.valueOf(annotatorId), true);
        taskStageSyncService.afterArbitrationConfirmed(taskId, td.getId());
    }

    @Transactional
    public void manual(ArbitrationSubmit body, HttpServletRequest request) {
        long arbitratorId = currentUserService.requireCurrent(request).id;
        TaskDocument td = taskDocumentResolver.requireTaskDocument((int) body.taskId(), body.dataId());
        annotationPersistenceService.saveArbitration((int) body.taskId(), td.getId(), arbitratorId,
                body.propositions() == null ? List.of() : body.propositions(),
                body.relations() == null ? List.of() : body.relations(), true, body.graphLayout());
        upsertSnapshot((int) body.taskId(), td.getId(), arbitratorId, "MANUAL", false);
    }

    @Transactional
    public void confirm(Map<String, Object> body, HttpServletRequest request) {
        int taskId = (int) MapBodyUtils.longValue(body.get("taskId"), 0);
        long dataId = MapBodyUtils.longValue(body.get("dataId"), MapBodyUtils.longValue(body.get("documentId"), 0));
        TaskDocument td = taskDocumentResolver.requireTaskDocument(taskId, dataId);
        requireAllAnnotatorsSubmitted(taskId, td);
        ArbitrationSnapshot arb = arbitrationSnapshotMapper.selectByTaskAndDoc(taskId, td.getId());
        if (arb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无待确认的裁定结果");
        }
        if (arb.getFinalResult() != null && arb.getFinalResult() == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该文书裁定结果已确认");
        }
        currentUserService.requireCurrent(request);
        annotationPersistenceService.markArbitrationSubmitted(taskId, td.getId(), arb.getArbitratorId());
        arb.setFinalResult(1);
        arb.setArbitratedAt(LocalDateTime.now());
        arbitrationSnapshotMapper.update(arb);
        taskStageSyncService.afterArbitrationConfirmed(taskId, td.getId());
    }

    @Transactional
    public void reject(Map<String, Object> body, HttpServletRequest request) {
        int taskId = (int) MapBodyUtils.longValue(body.get("taskId"), 0);
        long dataId = MapBodyUtils.longValue(body.get("dataId"), MapBodyUtils.longValue(body.get("documentId"), 0));
        long annotatorId = MapBodyUtils.longValue(body.get("annotatorId"), 0);
        String reason = MapBodyUtils.text(body, "reason", "").trim();
        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写不予采纳理由");
        }
        TaskDocument td = taskDocumentResolver.requireTaskDocument(taskId, dataId);
        currentUserService.requireCurrent(request);
        annotationPersistenceService.rejectAnnotation(taskId, td.getId(), annotatorId, reason);

        ArbitrationSnapshot arb = arbitrationSnapshotMapper.selectByTaskAndDoc(taskId, td.getId());
        if (arb != null && (arb.getFinalResult() == null || arb.getFinalResult() != 1)) {
            annotationPersistenceService.deleteArbitration(taskId, td.getId(), arb.getArbitratorId());
            arbitrationSnapshotMapper.deleteByTaskAndDoc(taskId, td.getId());
        }
        taskStageSyncService.afterAnnotationRejected(taskId, td.getId());
    }

    @Transactional
    public void cancelPending(Map<String, Object> body) {
        int taskId = (int) MapBodyUtils.longValue(body.get("taskId"), 0);
        long dataId = MapBodyUtils.longValue(body.get("dataId"), MapBodyUtils.longValue(body.get("documentId"), 0));
        TaskDocument td = taskDocumentResolver.requireTaskDocument(taskId, dataId);
        ArbitrationSnapshot arb = arbitrationSnapshotMapper.selectByTaskAndDoc(taskId, td.getId());
        if (arb == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无待确认的裁定结果");
        }
        if (arb.getFinalResult() != null && arb.getFinalResult() == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已确认的裁定结果不可取消");
        }
        annotationPersistenceService.deleteArbitration(taskId, td.getId(), arb.getArbitratorId());
        arbitrationSnapshotMapper.deleteByTaskAndDoc(taskId, td.getId());
    }

    private void requireAllAnnotatorsSubmitted(int taskId, TaskDocument td) {
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        int annotatorCount = task.annotatorIds.size();
        if (annotatorCount <= 0) {
            return;
        }
        int submitted = annotationMapper.countSubmittedByTaskDocument(taskId, td.getId());
        if (submitted < annotatorCount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "尚有标注员未提交，无法确认最终裁定");
        }
    }

    private void upsertSnapshot(int taskId, int taskDocumentId, long arbitratorId, String adoptedFrom,
                                boolean finalResult) {
        ArbitrationSnapshot existing = arbitrationSnapshotMapper.selectByTaskAndDoc(taskId, taskDocumentId);
        if (existing == null) {
            ArbitrationSnapshot snap = new ArbitrationSnapshot();
            snap.setTaskId(taskId);
            snap.setTaskDocumentId(taskDocumentId);
            snap.setArbitratorId(arbitratorId);
            snap.setAdoptedFrom(adoptedFrom);
            snap.setFinalResult(finalResult ? 1 : 0);
            snap.setArbitratedAt(LocalDateTime.now());
            arbitrationSnapshotMapper.insert(snap);
        } else {
            existing.setArbitratorId(arbitratorId);
            existing.setAdoptedFrom(adoptedFrom);
            existing.setFinalResult(finalResult ? 1 : 0);
            existing.setArbitratedAt(LocalDateTime.now());
            arbitrationSnapshotMapper.update(existing);
        }
    }
}
