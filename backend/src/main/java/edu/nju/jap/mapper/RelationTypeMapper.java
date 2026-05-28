package edu.nju.jap.mapper;

import edu.nju.jap.model.po.RelationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationTypeMapper {
    List<RelationType> selectByVersionId(@Param("versionId") int versionId);

    int deleteByVersionId(@Param("versionId") int versionId);

    int insert(RelationType type);
}
