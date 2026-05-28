package edu.nju.jap.mapper;

import edu.nju.jap.model.po.RelationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelationMapper {
    List<RelationPo> selectByScope(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId,
                                   @Param("userId") long userId);

    int deleteByScope(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId,
                      @Param("userId") long userId);

    int insert(RelationPo relation);
}
