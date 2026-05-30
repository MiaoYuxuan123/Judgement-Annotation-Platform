package edu.nju.jap.model.entity;

public class DocumentItem {
    public long id;
    public String documentId;
    public String title;
    public String type;
    public String uploadDate;
    public String content;
    public long uploadedBy;
    public String sourceType;
    public Long globalDocId;

    public DocumentItem(long id, String documentId, String title, String type, String uploadDate, String content,
                        long uploadedBy) {
        this(id, documentId, title, type, uploadDate, content, uploadedBy, null, null);
    }

    public DocumentItem(long id, String documentId, String title, String type, String uploadDate, String content,
                        long uploadedBy, String sourceType, Long globalDocId) {
        this.id = id;
        this.documentId = documentId;
        this.title = title;
        this.type = type;
        this.uploadDate = uploadDate;
        this.content = content;
        this.uploadedBy = uploadedBy;
        this.sourceType = sourceType;
        this.globalDocId = globalDocId;
    }
}
