package edu.nju.jap.model;

import java.time.LocalDateTime;
import java.util.List;

public class TaskItem {
    public long id;
    public String taskName;
    public String description;
    public String status;
    public long configId;
    public List<Long> documentIds;
    public List<Long> annotatorIds;
    public long reviewerId;
    public long creatorId;
    public LocalDateTime createdAt;
    public LocalDateTime stageChangedAt;
    public GuideConfig configSnapshot;

    public TaskItem(long id, String taskName, String description, String status, long configId,
                    List<Long> documentIds, List<Long> annotatorIds, long reviewerId, long creatorId,
                    LocalDateTime createdAt, GuideConfig configSnapshot) {
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.status = status;
        this.configId = configId;
        this.documentIds = documentIds;
        this.annotatorIds = annotatorIds;
        this.reviewerId = reviewerId;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
        this.stageChangedAt = createdAt;
        this.configSnapshot = configSnapshot;
    }
}
