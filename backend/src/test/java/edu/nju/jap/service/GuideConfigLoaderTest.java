package edu.nju.jap.service;

import edu.nju.jap.mapper.GuideVersionMapper;
import edu.nju.jap.mapper.LabelL1Mapper;
import edu.nju.jap.mapper.LabelL2Mapper;
import edu.nju.jap.mapper.RelationTypeMapper;
import edu.nju.jap.model.entity.GuideConfig;
import edu.nju.jap.model.po.GuideVersion;
import edu.nju.jap.service.support.GuideConfigLoader;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GuideConfigLoaderTest {
    private final GuideVersionMapper guideVersionMapper = mock(GuideVersionMapper.class);
    private final LabelL1Mapper labelL1Mapper = mock(LabelL1Mapper.class);
    private final LabelL2Mapper labelL2Mapper = mock(LabelL2Mapper.class);
    private final RelationTypeMapper relationTypeMapper = mock(RelationTypeMapper.class);
    private final GuideConfigLoader loader = new GuideConfigLoader(
            guideVersionMapper, labelL1Mapper, labelL2Mapper, relationTypeMapper);

    @Test
    void loadExistingVersion() {
        GuideVersion version = new GuideVersion();
        version.setId(1);
        version.setVersionName("v1");
        version.setDescription("guide");
        version.setCreatedAt(LocalDateTime.now());
        when(guideVersionMapper.selectById(1)).thenReturn(version);
        when(labelL1Mapper.selectByVersionId(1)).thenReturn(List.of());
        when(labelL2Mapper.selectByVersionId(1)).thenReturn(List.of());
        when(relationTypeMapper.selectByVersionId(1)).thenReturn(List.of());

        GuideConfig config = loader.load(1);

        assertThat(config.id).isEqualTo(1);
        assertThat(config.versionName).isEqualTo("v1");
        assertThat(config.primaryTags).isEmpty();
    }

    @Test
    void loadMissingVersionThrowsNotFound() {
        when(guideVersionMapper.selectById(999)).thenReturn(null);

        assertThatThrownBy(() -> loader.load(999))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).isEqualTo("指南版本不存在");
                });
    }
}
