package edu.nju.jap.service.support;

import edu.nju.jap.mapper.AnnotationMapper;
import edu.nju.jap.mapper.TaskDocumentMapper;
import edu.nju.jap.mapper.TaskMapper;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.model.po.TaskDocument;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据标注/裁定进度同步任务阶段（标注中 → 待裁定 → 可导出）。
 * 文书「待裁定」按标注员个人进度在前端计算；此处仅维护全局任务/文书可导出状态。
 */
@Component
public class TaskStageSyncService {
    public static final String STAGE_ANNOTATING = "标注中";
    public static final String STAGE_ARBITRATION = "待裁定";
    public static final String STAGE_EXPORTABLE = "可导出";

    private final AnnotationMapper annotationMapper;
    private final TaskDocumentMapper taskDocumentMapper;
    private final TaskMapper taskMapper;
    private final TaskAggregateService taskAggregateService;

    public TaskStageSyncService(AnnotationMapper annotationMapper, TaskDocumentMapper taskDocumentMapper,
                                  TaskMapper taskMapper, TaskAggregateService taskAggregateService) {
        this.annotationMapper = annotationMapper;
        this.taskDocumentMapper = taskDocumentMapper;
        this.taskMapper = taskMapper;
        this.taskAggregateService = taskAggregateService;
    }

    /**
     * 标注提交（非草稿）后：当所有标注员均已完成全部文书标注时，任务进入「待裁定」（供裁定者处理）。
     */
    public void afterAnnotationSubmitted(int taskId) {
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        int annotatorCount = task.annotatorIds.size();
        if (annotatorCount <= 0 || !STAGE_ANNOTATING.equals(task.status)) {
            return;
        }

        List<TaskDocument> docs = taskDocumentMapper.selectByTaskId(taskId);
        boolean allAnnotatorsFinishedAllDocs = !docs.isEmpty() && docs.stream()
                .allMatch(doc -> annotationMapper.countSubmittedByTaskDocument(taskId, doc.getId()) >= annotatorCount);
        if (allAnnotatorsFinishedAllDocs) {
            taskMapper.updateStatus(taskId, STAGE_ARBITRATION);
        }
    }

    /** 裁定确认后调用：更新文书为可导出。 */
    public void afterArbitrationConfirmed(int taskId, int taskDocumentId) {
        taskDocumentMapper.updateStatus(taskDocumentId, STAGE_EXPORTABLE);
    }
}
