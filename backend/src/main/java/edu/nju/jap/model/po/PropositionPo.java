package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class PropositionPo {
    private Long id;
    private Long annotationId;
    private String displayId;
    private Integer sequenceNo;
    private Integer startPos;
    private Integer endPos;
    private String selectedText;
    private String labelL1;
    private String labelL2;
    private String labelPath;
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
    public Integer getStartPos() { return startPos; }
    public void setStartPos(Integer startPos) { this.startPos = startPos; }
    public Integer getEndPos() { return endPos; }
    public void setEndPos(Integer endPos) { this.endPos = endPos; }
    public String getSelectedText() { return selectedText; }
    public void setSelectedText(String selectedText) { this.selectedText = selectedText; }
    public String getLabelL1() { return labelL1; }
    public void setLabelL1(String labelL1) { this.labelL1 = labelL1; }
    public String getLabelL2() { return labelL2; }
    public void setLabelL2(String labelL2) { this.labelL2 = labelL2; }
    public String getLabelPath() { return labelPath; }
    public void setLabelPath(String labelPath) { this.labelPath = labelPath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
