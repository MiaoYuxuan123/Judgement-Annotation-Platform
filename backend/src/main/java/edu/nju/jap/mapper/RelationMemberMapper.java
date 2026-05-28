package edu.nju.jap.mapper;

import edu.nju.jap.model.po.RelationMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationMemberMapper {
    List<RelationMember> selectByRelationId(@Param("relationId") long relationId);

    int deleteByAnnotationId(@Param("annotationId") long annotationId);

    int insert(RelationMember member);
}
