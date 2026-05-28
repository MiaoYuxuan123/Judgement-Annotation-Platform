package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class PropositionPo {
    private Integer id;
    private Integer taskId;
    private Integer taskDocumentId;
    private Long userId;
    private String displayId;
    private String content;
    private Integer startOffset;
    private Integer endOffset;
    private String labelL1;
    private String labelL2;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

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
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getStartOffset() { return startOffset; }
    public void setStartOffset(Integer startOffset) { this.startOffset = startOffset; }
    public Integer getEndOffset() { return endOffset; }
    public void setEndOffset(Integer endOffset) { this.endOffset = endOffset; }
    public String getLabelL1() { return labelL1; }
    public void setLabelL1(String labelL1) { this.labelL1 = labelL1; }
    public String getLabelL2() { return labelL2; }
    public void setLabelL2(String labelL2) { this.labelL2 = labelL2; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
