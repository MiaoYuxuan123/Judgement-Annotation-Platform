package edu.nju.jap.model.po;

public class LabelL2 {
    private Integer id;
    private Integer guideVersionId;
    private Integer parentL1Id;
    private String name;
    private String abbr;
    private String description;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getGuideVersionId() { return guideVersionId; }
    public void setGuideVersionId(Integer guideVersionId) { this.guideVersionId = guideVersionId; }
    public Integer getParentL1Id() { return parentL1Id; }
    public void setParentL1Id(Integer parentL1Id) { this.parentL1Id = parentL1Id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAbbr() { return abbr; }
    public void setAbbr(String abbr) { this.abbr = abbr; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
