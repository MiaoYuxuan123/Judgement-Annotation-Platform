package edu.nju.jap.mapper;

import edu.nju.jap.model.po.TaskMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMemberMapper {
    List<TaskMember> selectByTaskId(@Param("taskId") int taskId);

    int insert(TaskMember member);

    int deleteByTaskId(@Param("taskId") int taskId);
}
