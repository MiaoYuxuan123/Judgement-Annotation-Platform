package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class TaskDocument {
    private Integer id;
    private Integer taskId;
    private String sourceType;
    private Long globalDocId;
    private String fileName;
    private String filePath;
    private String extractedText;
    private LocalDateTime uploadedAt;
    private String status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getGlobalDocId() { return globalDocId; }
    public void setGlobalDocId(Long globalDocId) { this.globalDocId = globalDocId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
