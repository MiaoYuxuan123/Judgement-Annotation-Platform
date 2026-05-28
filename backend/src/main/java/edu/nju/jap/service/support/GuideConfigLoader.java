package edu.nju.jap.service.support;

import edu.nju.jap.mapper.GuideVersionMapper;
import edu.nju.jap.mapper.LabelL1Mapper;
import edu.nju.jap.mapper.LabelL2Mapper;
import edu.nju.jap.mapper.RelationTypeMapper;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.model.po.GuideVersion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GuideConfigLoader {
    private final GuideVersionMapper guideVersionMapper;
    private final LabelL1Mapper labelL1Mapper;
    private final LabelL2Mapper labelL2Mapper;
    private final RelationTypeMapper relationTypeMapper;

    public GuideConfigLoader(GuideVersionMapper guideVersionMapper, LabelL1Mapper labelL1Mapper,
                             LabelL2Mapper labelL2Mapper, RelationTypeMapper relationTypeMapper) {
        this.guideVersionMapper = guideVersionMapper;
        this.labelL1Mapper = labelL1Mapper;
        this.labelL2Mapper = labelL2Mapper;
        this.relationTypeMapper = relationTypeMapper;
    }

    public GuideConfig load(int versionId) {
        GuideVersion version = guideVersionMapper.selectById(versionId);
        if (version == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "指南版本不存在");
        }
        return DomainConverter.toGuideConfig(version,
                labelL1Mapper.selectByVersionId(versionId),
                labelL2Mapper.selectByVersionId(versionId),
                relationTypeMapper.selectByVersionId(versionId));
    }

    public GuideConfig loadActive() {
        GuideVersion version = guideVersionMapper.selectActive();
        if (version == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未配置启用的指南版本");
        }
        return load(version.getId());
    }
}
