package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class AnnotationPo {
    private Long id;
    private Long taskId;
    private Long documentId;
    private Long userId;
    private String status;
    private Integer isFinal;
    private Long guideVersionId;
    private String guideSnapshot;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getIsFinal() { return isFinal; }
    public void setIsFinal(Integer isFinal) { this.isFinal = isFinal; }
    public Long getGuideVersionId() { return guideVersionId; }
    public void setGuideVersionId(Long guideVersionId) { this.guideVersionId = guideVersionId; }
    public String getGuideSnapshot() { return guideSnapshot; }
    public void setGuideSnapshot(String guideSnapshot) { this.guideSnapshot = guideSnapshot; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
