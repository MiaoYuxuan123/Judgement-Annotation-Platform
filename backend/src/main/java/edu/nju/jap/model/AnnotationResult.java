package edu.nju.jap.model;

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

    public AnnotationResult(long taskId, long dataId, long userId, List<Proposition> propositions,
                            List<Relation> relations, boolean draft, LocalDateTime submittedAt) {
        this.taskId = taskId;
        this.dataId = dataId;
        this.userId = userId;
        this.propositions = propositions;
        this.relations = relations;
        this.draft = draft;
        this.submittedAt = submittedAt;
    }
}
