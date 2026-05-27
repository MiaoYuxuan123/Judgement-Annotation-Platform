package edu.nju.jap.model.entity;

import java.time.LocalDateTime;
import java.util.List;

public class ArbitrationResult {
    public long taskId;
    public long dataId;
    public long arbitratorId;
    public List<Proposition> propositions;
    public List<Relation> relations;
    public String adoptedFrom;
    public LocalDateTime arbitratedAt;
    public boolean finalResult = true;

    public ArbitrationResult(long taskId, long dataId, long arbitratorId, List<Proposition> propositions,
                             List<Relation> relations, String adoptedFrom, LocalDateTime arbitratedAt) {
        this.taskId = taskId;
        this.dataId = dataId;
        this.arbitratorId = arbitratorId;
        this.propositions = propositions;
        this.relations = relations;
        this.adoptedFrom = adoptedFrom;
        this.arbitratedAt = arbitratedAt;
    }
}
