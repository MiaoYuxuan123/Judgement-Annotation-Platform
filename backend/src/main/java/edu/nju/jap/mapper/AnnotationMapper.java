package edu.nju.jap.mapper;

import edu.nju.jap.model.po.AnnotationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnotationMapper {
    AnnotationPo selectByScope(@Param("taskId") long taskId, @Param("documentId") long documentId,
                               @Param("userId") long userId);

    int insert(AnnotationPo annotation);

    int updateStatus(AnnotationPo annotation);

    int countSubmittedByTaskDocument(@Param("taskId") int taskId, @Param("documentId") int documentId);
}
