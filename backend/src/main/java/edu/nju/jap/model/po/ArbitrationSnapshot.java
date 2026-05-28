package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class ArbitrationSnapshot {
    private Integer id;
    private Integer taskId;
    private Integer taskDocumentId;
    private Long arbitratorId;
    private String adoptedFrom;
    private Integer finalResult;
    private LocalDateTime arbitratedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public Integer getTaskDocumentId() { return taskDocumentId; }
    public void setTaskDocumentId(Integer taskDocumentId) { this.taskDocumentId = taskDocumentId; }
    public Long getArbitratorId() { return arbitratorId; }
    public void setArbitratorId(Long arbitratorId) { this.arbitratorId = arbitratorId; }
    public String getAdoptedFrom() { return adoptedFrom; }
    public void setAdoptedFrom(String adoptedFrom) { this.adoptedFrom = adoptedFrom; }
    public Integer getFinalResult() { return finalResult; }
    public void setFinalResult(Integer finalResult) { this.finalResult = finalResult; }
    public LocalDateTime getArbitratedAt() { return arbitratedAt; }
    public void setArbitratedAt(LocalDateTime arbitratedAt) { this.arbitratedAt = arbitratedAt; }
}
