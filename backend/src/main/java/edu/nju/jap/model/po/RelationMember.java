package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class RelationMember {
    private Long id;
    private Long relationId;
    private String memberType;
    private Long propositionId;
    private Long childRelationId;
    private String memberRole;
    private Integer memberOrder;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRelationId() { return relationId; }
    public void setRelationId(Long relationId) { this.relationId = relationId; }
    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public Long getPropositionId() { return propositionId; }
    public void setPropositionId(Long propositionId) { this.propositionId = propositionId; }
    public Long getChildRelationId() { return childRelationId; }
    public void setChildRelationId(Long childRelationId) { this.childRelationId = childRelationId; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public Integer getMemberOrder() { return memberOrder; }
    public void setMemberOrder(Integer memberOrder) { this.memberOrder = memberOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
