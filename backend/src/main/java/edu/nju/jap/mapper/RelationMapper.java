package edu.nju.jap.mapper;

import edu.nju.jap.model.po.RelationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationMapper {
    List<RelationPo> selectByAnnotationId(@Param("annotationId") long annotationId);

    int deleteByAnnotationId(@Param("annotationId") long annotationId);

    int insert(RelationPo relation);
}
