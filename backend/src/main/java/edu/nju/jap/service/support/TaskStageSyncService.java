package edu.nju.jap.service.support;

import edu.nju.jap.mapper.AnnotationMapper;
import edu.nju.jap.mapper.GlobalDocumentMapper;
import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.mapper.TaskDocumentMapper;
import edu.nju.jap.mapper.TaskMapper;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.model.po.GlobalDocument;
import edu.nju.jap.model.po.Task;
import edu.nju.jap.model.po.TaskDocument;
import edu.nju.jap.service.MessageService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据标注/裁定进度同步任务与文书阶段（标注中 → 待裁定 → 可导出）。
 * 每条文书独立判断：全员提交后可进入「待裁定」；裁定确认后该文书「可导出」。
 */
@Component
public class TaskStageSyncService {
    public static final String STAGE_ANNOTATING = "标注中";
    public static final String STAGE_ARBITRATION = "待裁定";
    public static final String STAGE_EXPORTABLE = "可导出";

    private final AnnotationMapper annotationMapper;
    private final TaskDocumentMapper taskDocumentMapper;
    private final TaskMapper taskMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskAggregateService taskAggregateService;
    private final MessageService messageService;
    private final GlobalDocumentMapper globalDocumentMapper;

    public TaskStageSyncService(AnnotationMapper annotationMapper, TaskDocumentMapper taskDocumentMapper,
                                  TaskMapper taskMapper, SysUserMapper sysUserMapper,
                                  TaskAggregateService taskAggregateService,
                                  MessageService messageService, GlobalDocumentMapper globalDocumentMapper) {
        this.annotationMapper = annotationMapper;
        this.taskDocumentMapper = taskDocumentMapper;
        this.taskMapper = taskMapper;
        this.sysUserMapper = sysUserMapper;
        this.taskAggregateService = taskAggregateService;
        this.messageService = messageService;
        this.globalDocumentMapper = globalDocumentMapper;
    }

    /**
     * 标注提交（非草稿）后：逐条文书检查是否全员已提交，满足则该文书进入「待裁定」并同步任务阶段。
     */
    public void afterAnnotationSubmitted(int taskId) {
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        int annotatorCount = taskAggregateService.countActiveAnnotators(task);
        if (annotatorCount <= 0) {
            return;
        }

        List<TaskDocument> docs = taskDocumentMapper.selectByTaskId(taskId);
        for (TaskDocument doc : docs) {
            if (STAGE_EXPORTABLE.equals(doc.getStatus()) || STAGE_ARBITRATION.equals(doc.getStatus())) {
                continue;
            }
            if (annotationMapper.countSubmittedByTaskDocument(taskId, doc.getId()) >= annotatorCount) {
                taskDocumentMapper.updateStatus(doc.getId(), STAGE_ARBITRATION);
                String docTitle = resolveDocTitle(doc);
                int msgDataId = resolveDataId(doc);
                messageService.send(task.reviewerId, "SUBMISSION", "请进行裁定",
                        "文书【" + docTitle + "】已全员提交，请进行裁定", taskId, doc.getId(), msgDataId);
                messageService.send(task.creatorId, "SUBMISSION", "待裁定",
                        "文书【" + docTitle + "】已全员提交，状态变更为待裁定", taskId, doc.getId(), msgDataId);
            }
        }
        syncTaskStatus(taskId);
    }

    /** 账号恢复后同步任务状态：排除该用户，按其余活跃标注员判断 */ 
    public void syncTasksExcludingUser(int taskId, long excludedUserId) {
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        int annotatorCount = (int) task.annotatorIds.stream()
                .filter(id -> id != excludedUserId)
                .filter(id -> {
                    edu.nju.jap.model.po.SysUser u = sysUserMapper.selectById(id);
                    return u != null && (u.getIsDeleted() == null || u.getIsDeleted() != 1);
                })
                .count();
        if (annotatorCount <= 0) return;

        List<TaskDocument> docs = taskDocumentMapper.selectByTaskId(taskId);
        for (TaskDocument doc : docs) {
            if (STAGE_EXPORTABLE.equals(doc.getStatus()) || STAGE_ARBITRATION.equals(doc.getStatus())) continue;
            if (annotationMapper.countSubmittedByTaskDocument(taskId, doc.getId()) >= annotatorCount) {
                taskDocumentMapper.updateStatus(doc.getId(), STAGE_ARBITRATION);
            }
        }
        syncTaskStatus(taskId);
    }

    /** 裁定确认后调用：更新该文书为可导出，并按全部文书进度同步任务阶段。 */
    public void afterArbitrationConfirmed(int taskId, int taskDocumentId) {
        taskDocumentMapper.updateStatus(taskDocumentId, STAGE_EXPORTABLE);
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        TaskDocument doc = taskDocumentMapper.selectById(taskDocumentId);
        if (doc != null) {
            String docTitle = resolveDocTitle(doc);
            int msgDataId = resolveDataId(doc);
            for (long annotatorId : task.annotatorIds) {
                messageService.send(annotatorId, "COMPLETION", "文书裁定完成",
                        "文书【" + docTitle + "】已完成裁定", taskId, taskDocumentId, msgDataId);
            }
        }
        syncTaskStatus(taskId);
        // 全部文书导出后任务完成提醒
        Task po = taskMapper.selectById(taskId);
        if (po != null && STAGE_EXPORTABLE.equals(po.getStatus())) {
            String taskTitle = po.getTitle() != null ? po.getTitle() : "任务#" + taskId;
            messageService.send(task.creatorId, "COMPLETION", "任务完成",
                    "任务【" + taskTitle + "】所有文书裁定已完成，可导出结果", taskId, null, null);
        }
    }

    /**
     * 裁定不予采纳后：若该文书尚未全员重新提交，则回退为「标注中」并同步任务阶段。
     */
    public void afterAnnotationRejected(int taskId, int taskDocumentId) {
        TaskItem task = taskAggregateService.loadTaskItem(taskId);
        int annotatorCount = taskAggregateService.countActiveAnnotators(task);
        if (annotatorCount <= 0) {
            return;
        }
        if (annotationMapper.countSubmittedByTaskDocument(taskId, taskDocumentId) < annotatorCount) {
            taskDocumentMapper.updateStatus(taskDocumentId, STAGE_ANNOTATING);
        }
        syncTaskStatus(taskId);
    }

    private void syncTaskStatus(int taskId) {
        List<TaskDocument> docs = taskDocumentMapper.selectByTaskId(taskId);
        if (docs.isEmpty()) {
            return;
        }
        if (docs.stream().allMatch(doc -> STAGE_EXPORTABLE.equals(doc.getStatus()))) {
            taskMapper.updateStatus(taskId, STAGE_EXPORTABLE);
            return;
        }
        if (docs.stream().anyMatch(doc -> STAGE_ARBITRATION.equals(doc.getStatus())
                || STAGE_EXPORTABLE.equals(doc.getStatus()))) {
            taskMapper.updateStatus(taskId, STAGE_ARBITRATION);
            return;
        }
        taskMapper.updateStatus(taskId, STAGE_ANNOTATING);
    }

    private String resolveDocTitle(TaskDocument td) {
        if (td.getGlobalDocId() != null) {
            GlobalDocument gd = globalDocumentMapper.selectById(td.getGlobalDocId());
            if (gd != null && gd.getTitle() != null && !gd.getTitle().isBlank()) return gd.getTitle();
        }
        return td.getFileName() != null ? td.getFileName() : "文书#" + td.getId();
    }

    private int resolveDataId(TaskDocument td) {
        if (td.getGlobalDocId() != null) {
            return td.getGlobalDocId().intValue();
        }
        return td.getId();
    }
}
