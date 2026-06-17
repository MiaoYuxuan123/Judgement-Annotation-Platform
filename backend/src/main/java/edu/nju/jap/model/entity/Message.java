package edu.nju.jap.model.entity;

import java.time.LocalDateTime;

public class Message {
    public long id;
    public long userId;
    public String type;
    public String title;
    public String content;
    public Integer taskId;
    public Integer taskDocumentId;
    public Integer dataId;
    public boolean isRead;
    public LocalDateTime createdAt;

    public Message() {}

    public Message(long userId, String type, String title, String content, Integer taskId, Integer taskDocumentId, Integer dataId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.taskId = taskId;
        this.taskDocumentId = taskDocumentId;
        this.dataId = dataId;
    }
}
