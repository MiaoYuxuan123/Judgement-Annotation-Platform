package edu.nju.jap.model.po;

import java.time.LocalDateTime;
import java.util.List;

public class RelationPo {
    private Integer id;
    private Integer taskId;
    private Integer taskDocumentId;
    private Long userId;
    private String displayId;
    private String type;
    private String targetType;
    private String targetId;
    private String expression;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<RelationMember> members;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public Integer getTaskDocumentId() { return taskDocumentId; }
    public void setTaskDocumentId(Integer taskDocumentId) { this.taskDocumentId = taskDocumentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<RelationMember> getMembers() { return members; }
    public void setMembers(List<RelationMember> members) { this.members = members; }
}
