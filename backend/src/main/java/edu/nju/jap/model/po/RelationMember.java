package edu.nju.jap.model.po;

public class RelationMember {
    private Integer id;
    private Integer relationId;
    private String sourceType;
    private String sourceId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRelationId() { return relationId; }
    public void setRelationId(Integer relationId) { this.relationId = relationId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
}
