package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class RelationPo {
    private Long id;
    private Long annotationId;
    private String displayId;
    private Integer sequenceNo;
    private String relationType;
    private String expression;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAnnotationId() { return annotationId; }
    public void setAnnotationId(Long annotationId) { this.annotationId = annotationId; }
    public String getDisplayId() { return displayId; }
    public void setDisplayId(String displayId) { this.displayId = displayId; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
