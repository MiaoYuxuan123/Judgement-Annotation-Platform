package edu.nju.jap.model.entity;

import java.time.LocalDateTime;
import java.util.List;

public class AnnotationResult {
    public long taskId;
    public long dataId;
    public long userId;
    public List<Proposition> propositions;
    public List<Relation> relations;
    public boolean draft;
    public LocalDateTime submittedAt;
    public String rejectReason;

    public AnnotationResult(long taskId, long dataId, long userId, List<Proposition> propositions,
                            List<Relation> relations, boolean draft, LocalDateTime submittedAt) {
        this(taskId, dataId, userId, propositions, relations, draft, submittedAt, null);
    }

    public AnnotationResult(long taskId, long dataId, long userId, List<Proposition> propositions,
                            List<Relation> relations, boolean draft, LocalDateTime submittedAt,
                            String rejectReason) {
        this.taskId = taskId;
        this.dataId = dataId;
        this.userId = userId;
        this.propositions = propositions;
        this.relations = relations;
        this.draft = draft;
        this.submittedAt = submittedAt;
        this.rejectReason = rejectReason;
    }
}
