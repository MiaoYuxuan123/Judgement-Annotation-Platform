package edu.nju.jap.mapper;

import edu.nju.jap.model.po.AnnotationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnnotationMapper {
    String RECORD_ANNOTATION = "ANNOTATION";
    String RECORD_ARBITRATION = "ARBITRATION";

    AnnotationPo selectByScope(@Param("taskId") long taskId, @Param("documentId") long documentId,
                               @Param("userId") long userId, @Param("recordType") String recordType);

    int insert(AnnotationPo annotation);

    int updateStatus(AnnotationPo annotation);

    int deleteById(@Param("id") long id);

    int countSubmittedByTaskDocument(@Param("taskId") int taskId, @Param("documentId") int documentId);
}
