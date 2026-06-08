package edu.nju.jap.service;

import edu.nju.jap.model.dto.request.AnnotationSubmit;
import edu.nju.jap.model.entity.Proposition;
import edu.nju.jap.model.entity.Relation;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.TaskDocument;
import edu.nju.jap.service.support.AnnotationPersistenceService;
import edu.nju.jap.service.support.CurrentUserService;
import edu.nju.jap.service.support.TaskDocumentResolver;
import edu.nju.jap.service.support.TaskStageSyncService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnnotationServiceTest {
    private final AnnotationPersistenceService persistenceService = mock(AnnotationPersistenceService.class);
    private final TaskDocumentResolver taskDocumentResolver = mock(TaskDocumentResolver.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final TaskStageSyncService taskStageSyncService = mock(TaskStageSyncService.class);
    private final AnnotationService annotationService = new AnnotationService(
            persistenceService, taskDocumentResolver, currentUserService, taskStageSyncService);

    @Test
    void submitRenumbersPropositionsByStartPosition() {
        MockHttpServletRequest request = requestWithUser();
        TaskDocument taskDocument = taskDocument();
        when(currentUserService.requireCurrent(request)).thenReturn(new User(3, "annotator1", "", "标注员一", "USER", false, "active"));
        when(taskDocumentResolver.requireTaskDocument(1001, 101)).thenReturn(taskDocument);

        AnnotationSubmit body = new AnnotationSubmit(1001, 101, List.of(
                new Proposition("P2", 1, 8, 12, "后", "SM"),
                new Proposition("P1", 2, 0, 4, "前", "GM-L")
        ), List.of(new Relation("R1", "S", "P1", "P2")), true);

        annotationService.submit(body, request);

        ArgumentCaptor<List<Proposition>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistenceService).saveAnnotation(eq(1001), eq(1), eq(3L), captor.capture(), anyList(), eq(true));
        assertThat(captor.getValue()).extracting(Proposition::propId).containsExactly("P1", "P2");
        assertThat(captor.getValue()).extracting(Proposition::sequenceNo).containsExactly(1, 2);
    }

    @Test
    void draftSubmitDoesNotAdvanceTaskStage() {
        MockHttpServletRequest request = requestWithUser();
        when(currentUserService.requireCurrent(request)).thenReturn(new User(3, "annotator1", "", "标注员一", "USER", false, "active"));
        when(taskDocumentResolver.requireTaskDocument(1001, 101)).thenReturn(taskDocument());

        annotationService.submit(new AnnotationSubmit(1001, 101, List.of(), List.of(), true), request);

        verify(taskStageSyncService, never()).afterAnnotationSubmitted(anyInt());
    }

    @Test
    void formalSubmitAdvancesTaskStageSync() {
        MockHttpServletRequest request = requestWithUser();
        when(currentUserService.requireCurrent(request)).thenReturn(new User(3, "annotator1", "", "标注员一", "USER", false, "active"));
        when(taskDocumentResolver.requireTaskDocument(1001, 101)).thenReturn(taskDocument());

        annotationService.submit(new AnnotationSubmit(1001, 101, List.of(), List.of(), false), request);

        verify(taskStageSyncService).afterAnnotationSubmitted(1001);
    }

    private static MockHttpServletRequest requestWithUser() {
        return new MockHttpServletRequest();
    }

    private static TaskDocument taskDocument() {
        TaskDocument taskDocument = new TaskDocument();
        taskDocument.setId(1);
        taskDocument.setTaskId(1001);
        taskDocument.setGlobalDocId(101L);
        taskDocument.setSourceType("GLOBAL");
        return taskDocument;
    }
}
