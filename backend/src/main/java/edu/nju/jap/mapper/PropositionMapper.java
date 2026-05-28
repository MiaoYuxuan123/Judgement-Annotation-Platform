package edu.nju.jap.mapper;

import edu.nju.jap.model.po.PropositionPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PropositionMapper {
    List<PropositionPo> selectByScope(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId,
                                      @Param("userId") long userId);

    int deleteByScope(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId,
                      @Param("userId") long userId);

    int insert(PropositionPo proposition);
}
