package edu.nju.jap.service;

import edu.nju.jap.model.dto.request.AnnotationSubmit;
import edu.nju.jap.model.dto.request.GraphLayoutSave;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.po.Task;
import edu.nju.jap.service.support.AnnotationPersistenceService;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.TaskAggregateService;
import edu.nju.jap.service.support.TaskDocumentResolver;
import edu.nju.jap.service.support.TaskStageSyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnnotationService {
    private final AnnotationPersistenceService annotationPersistenceService;
    private final TaskDocumentResolver taskDocumentResolver;
    private final CurrentUserService currentUserService;
    private final TaskStageSyncService taskStageSyncService;
    private final TaskAggregateService taskAggregateService;

    public AnnotationService(AnnotationPersistenceService annotationPersistenceService,
                             TaskDocumentResolver taskDocumentResolver,
                             CurrentUserService currentUserService,
                             TaskStageSyncService taskStageSyncService,
                             TaskAggregateService taskAggregateService) {
        this.annotationPersistenceService = annotationPersistenceService;
        this.taskDocumentResolver = taskDocumentResolver;
        this.currentUserService = currentUserService;
        this.taskStageSyncService = taskStageSyncService;
        this.taskAggregateService = taskAggregateService;
    }

    @Transactional
    public void submit(AnnotationSubmit body, HttpServletRequest request) {
        Task task = taskAggregateService.requireTaskPo((int) body.taskId());
        if (task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "任务已截止，无法提交标注");
        }
        long userId = currentUserService.requireCurrent(request).id;
        var td = taskDocumentResolver.requireTaskDocument((int) body.taskId(), body.dataId());
        List<Proposition> propositions = new ArrayList<>(body.propositions() == null ? List.of() : body.propositions());
        propositions.sort(Comparator.comparingInt(Proposition::startPos));
        for (int i = 0; i < propositions.size(); i++) {
            Proposition p = propositions.get(i);
            propositions.set(i, new Proposition(p.propId(), i + 1, p.startPos(), p.endPos(), p.text(), p.tag()));
        }
        annotationPersistenceService.saveAnnotation((int) body.taskId(), td.getId(), userId, propositions,
                body.relations() == null ? List.of() : body.relations(), body.isDraft(), body.graphLayout());
        if (!body.isDraft()) {
            taskStageSyncService.afterAnnotationSubmitted((int) body.taskId());
        }
    }

    @Transactional
    public void saveLayout(GraphLayoutSave body, HttpServletRequest request) {
        long userId = currentUserService.requireCurrent(request).id;
        var td = taskDocumentResolver.requireTaskDocument((int) body.taskId(), body.dataId());
        annotationPersistenceService.saveGraphLayout((int) body.taskId(), td.getId(), userId, body.graphLayout(),
                AnnotationPersistenceService.RECORD_ANNOTATION);
    }
}
