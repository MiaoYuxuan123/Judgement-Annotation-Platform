package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class Task {
    private Integer id;
    private String title;
    private String description;
    private String status;
    private Long creatorId;
    private Integer guideVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime stageChangedAt;
    private LocalDateTime deadline;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Integer getGuideVersionId() { return guideVersionId; }
    public void setGuideVersionId(Integer guideVersionId) { this.guideVersionId = guideVersionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getStageChangedAt() { return stageChangedAt; }
    public void setStageChangedAt(LocalDateTime stageChangedAt) { this.stageChangedAt = stageChangedAt; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
}
