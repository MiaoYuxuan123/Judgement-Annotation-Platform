package edu.nju.jap.mapper;

import edu.nju.jap.model.po.TaskDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskDocumentMapper {
    TaskDocument selectById(@Param("id") int id);

    TaskDocument selectByTaskAndDataId(@Param("taskId") int taskId, @Param("dataId") long dataId);

    List<TaskDocument> selectByTaskId(@Param("taskId") int taskId);

    int insert(TaskDocument doc);

    int deleteByTaskId(@Param("taskId") int taskId);
}
