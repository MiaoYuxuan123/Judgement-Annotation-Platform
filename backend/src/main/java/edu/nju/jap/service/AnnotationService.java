package edu.nju.jap.service;

import edu.nju.jap.model.dto.request.AnnotationSubmit;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.service.support.AnnotationPersistenceService;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.TaskDocumentResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AnnotationService {
    private final AnnotationPersistenceService annotationPersistenceService;
    private final TaskDocumentResolver taskDocumentResolver;
    private final CurrentUserService currentUserService;

    public AnnotationService(AnnotationPersistenceService annotationPersistenceService,
                             TaskDocumentResolver taskDocumentResolver,
                             CurrentUserService currentUserService) {
        this.annotationPersistenceService = annotationPersistenceService;
        this.taskDocumentResolver = taskDocumentResolver;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public void submit(AnnotationSubmit body, HttpServletRequest request) {
        long userId = currentUserService.requireCurrent(request).id;
        var td = taskDocumentResolver.requireTaskDocument((int) body.taskId(), body.dataId());
        List<Proposition> propositions = new ArrayList<>(body.propositions() == null ? List.of() : body.propositions());
        propositions.sort(Comparator.comparingInt(Proposition::startPos));
        for (int i = 0; i < propositions.size(); i++) {
            Proposition p = propositions.get(i);
            propositions.set(i, new Proposition(p.propId(), i + 1, p.startPos(), p.endPos(), p.text(), p.tag()));
        }
        annotationPersistenceService.saveAnnotation((int) body.taskId(), td.getId(), userId, propositions,
                body.relations() == null ? List.of() : body.relations(), body.isDraft());
    }
}
