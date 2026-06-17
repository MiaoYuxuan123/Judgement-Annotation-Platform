package edu.nju.jap.service;

import edu.nju.jap.mapper.*;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.service.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TaskServiceTest {
    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TaskMemberMapper taskMemberMapper = mock(TaskMemberMapper.class);
    private final TaskDocumentMapper taskDocumentMapper = mock(TaskDocumentMapper.class);
    private final GlobalDocumentMapper globalDocumentMapper = mock(GlobalDocumentMapper.class);
    private final TaskAggregateService taskAggregateService = mock(TaskAggregateService.class);
    private final TaskDocumentResolver taskDocumentResolver = mock(TaskDocumentResolver.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final AnnotationPersistenceService annotationPersistenceService = mock(AnnotationPersistenceService.class);
    private final TaskDocumentFactory taskDocumentFactory = mock(TaskDocumentFactory.class);
    private final ArbitrationSnapshotMapper arbitrationSnapshotMapper = mock(ArbitrationSnapshotMapper.class);
    private final AnnotationMapper annotationMapper = mock(AnnotationMapper.class);
    private final TaskDocumentStorage taskDocumentStorage = mock(TaskDocumentStorage.class);
    private final MessageService messageService = mock(MessageService.class);
    private final TaskService taskService = new TaskService(taskMapper, taskMemberMapper, taskDocumentMapper,
            globalDocumentMapper, taskAggregateService, taskDocumentResolver, currentUserService,
            annotationPersistenceService, taskDocumentFactory, arbitrationSnapshotMapper, annotationMapper,
            taskDocumentStorage, messageService);

    @Test
    void nextStatusOnlyMovesForward() {
        assertThat(TaskService.nextStatus("标注中")).isEqualTo("待裁定");
        assertThat(TaskService.nextStatus("待裁定")).isEqualTo("可导出");
        assertThat(TaskService.nextStatus("可导出")).isEqualTo("可导出");
    }

    @Test
    void advancingToPreviousStageThrowsConflict() {
        when(taskAggregateService.loadTaskItem(1001)).thenReturn(task("待裁定"));

        assertThatThrownBy(() -> taskService.advance(1001, Map.of("status", "标注中")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("任务阶段不可回退");
                });
        verify(taskMapper, never()).updateStatus(anyInt(), anyString());
    }

    private static TaskItem task(String status) {
        return new TaskItem(1001, "任务", "描述", status, 1,
                List.of(101L), List.of(3L, 4L), 5L, 2L, LocalDateTime.now(), null);
    }
}
