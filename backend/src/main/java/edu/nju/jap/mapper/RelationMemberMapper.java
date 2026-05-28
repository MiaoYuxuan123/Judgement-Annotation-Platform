package edu.nju.jap.mapper;

import edu.nju.jap.model.po.RelationMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationMemberMapper {
    List<RelationMember> selectByRelationId(@Param("relationId") int relationId);

    int deleteByRelationId(@Param("relationId") int relationId);

    int insert(RelationMember member);
}
