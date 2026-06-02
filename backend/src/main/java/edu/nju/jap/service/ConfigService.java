package edu.nju.jap.service;

import edu.nju.jap.common.MapBodyUtils;
import edu.nju.jap.mapper.GuideVersionMapper;
import edu.nju.jap.mapper.LabelL1Mapper;
import edu.nju.jap.mapper.LabelL2Mapper;
import edu.nju.jap.mapper.RelationTypeMapper;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.model.entity.LabelDef;
import edu.nju.jap.model.po.GuideVersion;
import edu.nju.jap.model.po.LabelL1;
import edu.nju.jap.model.po.LabelL2;
import edu.nju.jap.model.po.RelationType;
import edu.nju.jap.service.support.GuideConfigLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ConfigService {
    private final GuideVersionMapper guideVersionMapper;
    private final LabelL1Mapper labelL1Mapper;
    private final LabelL2Mapper labelL2Mapper;
    private final RelationTypeMapper relationTypeMapper;
    private final GuideConfigLoader guideConfigLoader;

    public ConfigService(GuideVersionMapper guideVersionMapper, LabelL1Mapper labelL1Mapper,
                         LabelL2Mapper labelL2Mapper, RelationTypeMapper relationTypeMapper,
                         GuideConfigLoader guideConfigLoader) {
        this.guideVersionMapper = guideVersionMapper;
        this.labelL1Mapper = labelL1Mapper;
        this.labelL2Mapper = labelL2Mapper;
        this.relationTypeMapper = relationTypeMapper;
        this.guideConfigLoader = guideConfigLoader;
    }

    public List<GuideConfig> list() {
        return guideVersionMapper.selectAll().stream()
                .map(v -> guideConfigLoader.load(v.getId()))
                .sorted(Comparator.comparing(c -> c.id))
                .toList();
    }

    public GuideConfig active() {
        return guideConfigLoader.loadActive();
    }

    @Transactional
    public long create(Map<String, Object> body) {
        GuideConfig base = loadBaseOrEmpty();
        GuideVersion version = new GuideVersion();
        version.setVersionName(MapBodyUtils.text(body, "versionName", "V-new"));
        version.setDescription(MapBodyUtils.text(body, "description", "自定义指南版本"));
        guideVersionMapper.insert(version);
        saveLabels(version.getId(), labelDefs(body.get("primaryTags"), base.primaryTags),
                labelDefs(body.get("secondaryTags"), base.secondaryTags),
                labelDefs(body.get("relationTypes"), base.relationTypes));
        return version.getId();
    }

    @Transactional
    public GuideConfig update(long id, Map<String, Object> body) {
        GuideConfig old = requireConfig(id);
        GuideVersion version = guideVersionMapper.selectById((int) id);
        version.setVersionName(MapBodyUtils.text(body, "versionName", old.versionName));
        version.setDescription(MapBodyUtils.text(body, "description", old.description));
        guideVersionMapper.update(version);
        labelL1Mapper.deleteByVersionId((int) id);
        labelL2Mapper.deleteByVersionId((int) id);
        relationTypeMapper.deleteByVersionId((int) id);
        saveLabels((int) id, labelDefs(body.get("primaryTags"), old.primaryTags),
                labelDefs(body.get("secondaryTags"), old.secondaryTags),
                labelDefs(body.get("relationTypes"), old.relationTypes));
        return guideConfigLoader.load((int) id);
    }

    public void delete(long id) {
        if (guideVersionMapper.deleteById((int) id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "指南版本不存在");
        }
    }

    public GuideConfig requireConfig(long id) {
        return guideConfigLoader.load((int) id);
    }

    private GuideConfig loadBaseOrEmpty() {
        GuideVersion baseVersion = guideVersionMapper.selectById(1);
        if (baseVersion == null) {
            return new GuideConfig(0, "", "", false, "", List.of(), List.of(), List.of());
        }
        return guideConfigLoader.load(1);
    }

    private void saveLabels(int versionId, List<LabelDef> primary, List<LabelDef> secondary, List<LabelDef> relations) {
        Map<String, Integer> l1IdByAbbr = new java.util.HashMap<>();
        for (LabelDef def : primary) {
            LabelL1 l1 = new LabelL1();
            l1.setGuideVersionId(versionId);
            l1.setName(def.name());
            l1.setAbbr(def.shortName());
            l1.setDescription(def.description());
            labelL1Mapper.insert(l1);
            l1IdByAbbr.put(def.shortName(), l1.getId());
        }
        for (LabelDef def : secondary) {
            LabelL2 l2 = new LabelL2();
            l2.setGuideVersionId(versionId);
            l2.setParentL1Id(l1IdByAbbr.getOrDefault(def.parentTag(), l1IdByAbbr.values().stream().findFirst().orElse(1)));
            l2.setName(def.name());
            l2.setAbbr(def.shortName());
            l2.setDescription(def.description());
            labelL2Mapper.insert(l2);
        }
        for (LabelDef def : relations) {
            RelationType type = new RelationType();
            type.setGuideVersionId(versionId);
            type.setName(def.name());
            type.setAbbr(def.shortName());
            type.setDescription(def.description());
            type.setIsBinary(1);
            relationTypeMapper.insert(type);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<LabelDef> labelDefs(Object value, List<LabelDef> fallback) {
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> raw)) {
            return fallback;
        }
        if (raw.isEmpty()) {
            return List.of();
        }
        List<LabelDef> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map<?, ?> m) {
                String sn = m.get("shortName") == null ? "" : m.get("shortName").toString();
                String nm = m.get("name") == null ? "" : m.get("name").toString();
                String desc = m.get("description") == null ? "" : m.get("description").toString();
                String parent = m.get("parentTag") == null ? "" : m.get("parentTag").toString();
                result.add(new LabelDef(sn, nm, desc, parent));
            }
        }
        return result.isEmpty() ? fallback : result;
    }
}
