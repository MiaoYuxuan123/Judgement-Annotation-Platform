package edu.nju.jap.mapper;

import edu.nju.jap.model.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Select("SELECT * FROM message WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Message> selectByUserId(@Param("userId") long userId);

    @Select("SELECT COUNT(*) FROM message WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") long userId);

    @Insert("INSERT INTO message (user_id, type, title, content, task_id, task_document_id, data_id, is_read, created_at) " +
            "VALUES (#{userId}, #{type}, #{title}, #{content}, #{taskId}, #{taskDocumentId}, #{dataId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Update("UPDATE message SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") long id, @Param("userId") long userId);

    @Update("UPDATE message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") long userId);

    @Delete("DELETE FROM message WHERE user_id = #{userId} AND is_read = 1")
    int deleteRead(@Param("userId") long userId);
}
