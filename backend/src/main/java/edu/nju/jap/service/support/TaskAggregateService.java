package edu.nju.jap.service.support;

import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.mapper.TaskDocumentMapper;
import edu.nju.jap.mapper.TaskMapper;
import edu.nju.jap.mapper.TaskMemberMapper;
import edu.nju.jap.model.dto.response.TaskSummary;
import edu.nju.jap.model.dto.response.UserVO;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.model.entity.TaskItem;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.Task;
import edu.nju.jap.model.po.TaskDocument;
import edu.nju.jap.model.po.TaskMember;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TaskAggregateService {
    private final TaskMapper taskMapper;
    private final TaskMemberMapper taskMemberMapper;
    private final TaskDocumentMapper taskDocumentMapper;
    private final SysUserMapper sysUserMapper;
    private final GuideConfigLoader guideConfigLoader;
    private final TaskDocumentResolver taskDocumentResolver;

    public TaskAggregateService(TaskMapper taskMapper, TaskMemberMapper taskMemberMapper,
                                TaskDocumentMapper taskDocumentMapper, SysUserMapper sysUserMapper,
                                GuideConfigLoader guideConfigLoader, TaskDocumentResolver taskDocumentResolver) {
        this.taskMapper = taskMapper;
        this.taskMemberMapper = taskMemberMapper;
        this.taskDocumentMapper = taskDocumentMapper;
        this.sysUserMapper = sysUserMapper;
        this.guideConfigLoader = guideConfigLoader;
        this.taskDocumentResolver = taskDocumentResolver;
    }

    public Task requireTaskPo(int id) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    public TaskItem loadTaskItem(int taskId) {
        Task task = requireTaskPo(taskId);
        List<TaskMember> members = taskMemberMapper.selectByTaskId(taskId);
        List<Long> annotatorIds = new ArrayList<>();
        long reviewerId = 0;
        for (TaskMember m : members) {
            if ("裁定者".equals(m.getRoleInTask())) {
                reviewerId = m.getUserId();
            } else {
                annotatorIds.add(m.getUserId());
            }
        }
        List<Long> documentIds = taskDocumentMapper.selectByTaskId(taskId).stream()
                .map(taskDocumentResolver::apiDataId)
                .toList();
        GuideConfig config = task.getGuideVersionId() == null ? null : guideConfigLoader.load(task.getGuideVersionId());
        TaskItem item = new TaskItem(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getGuideVersionId() == null ? 0 : task.getGuideVersionId(), documentIds, annotatorIds,
                reviewerId, task.getCreatorId(), task.getCreatedAt(), config);
        item.stageChangedAt = task.getStageChangedAt();
        return item;
    }

    public TaskSummary toSummary(TaskItem task) {
        User reviewer = DomainConverter.toUser(sysUserMapper.selectById(task.reviewerId));
        User creator = DomainConverter.toUser(sysUserMapper.selectById(task.creatorId));
        return new TaskSummary(task.id, task.taskName, task.description, task.status, task.documentIds.size(),
                task.annotatorIds.size(), reviewer == null ? "-" : reviewer.realName,
                creator == null ? "-" : creator.realName, task.createdAt);
    }

    public List<UserVO> annotatorVos(TaskItem task) {
        return task.annotatorIds.stream()
                .map(id -> UserVO.from(DomainConverter.toUser(sysUserMapper.selectById(id))))
                .toList();
    }

    public UserVO reviewerVo(TaskItem task) {
        return UserVO.from(DomainConverter.toUser(sysUserMapper.selectById(task.reviewerId)));
    }

    public List<TaskDocument> listTaskDocuments(int taskId) {
        return taskDocumentMapper.selectByTaskId(taskId);
    }
}
