package edu.nju.jap.model.entity;

import java.util.List;

public class GuideConfig {
    public long id;
    public String versionName;
    public String description;
    public boolean active;
    public String createdAt;
    public List<LabelDef> primaryTags;
    public List<LabelDef> secondaryTags;
    public List<LabelDef> relationTypes;

    public GuideConfig(long id, String versionName, String description, boolean active, String createdAt,
                       List<LabelDef> primaryTags, List<LabelDef> secondaryTags, List<LabelDef> relationTypes) {
        this.id = id;
        this.versionName = versionName;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.primaryTags = primaryTags;
        this.secondaryTags = secondaryTags;
        this.relationTypes = relationTypes;
    }
}
