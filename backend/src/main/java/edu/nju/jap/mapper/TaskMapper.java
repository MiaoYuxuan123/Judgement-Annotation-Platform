package edu.nju.jap.mapper;

import edu.nju.jap.model.po.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {
    Task selectById(@Param("id") int id);

    List<Task> selectAll(@Param("status") String status, @Param("keyword") String keyword);

    List<Task> selectByUserId(@Param("userId") long userId);

    int insert(Task task);

    int updateStatus(@Param("id") int id, @Param("status") String status);

    int countByGuideVersionId(@Param("id") int id);

    int deleteById(@Param("id") int id);
}
