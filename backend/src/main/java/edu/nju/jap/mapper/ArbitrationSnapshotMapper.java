package edu.nju.jap.mapper;

import edu.nju.jap.model.po.ArbitrationSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArbitrationSnapshotMapper {
    ArbitrationSnapshot selectByTaskAndDoc(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId);

    List<ArbitrationSnapshot> selectByTaskId(@Param("taskId") int taskId);

    int insert(ArbitrationSnapshot snapshot);

    int update(ArbitrationSnapshot snapshot);

    int deleteByTaskAndDoc(@Param("taskId") int taskId, @Param("taskDocumentId") int taskDocumentId);

    int deleteByTaskId(@Param("taskId") int taskId);
}
