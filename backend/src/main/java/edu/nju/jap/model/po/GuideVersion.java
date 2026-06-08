package edu.nju.jap.model.po;

import java.time.LocalDateTime;

public class GuideVersion {
    private Integer id;
    private String versionName;
    private String description;
    private LocalDateTime createdAt;
    private String attachmentName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }
}