package edu.nju.jap.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthTokenMapper {
    int insert(@Param("token") String token, @Param("userId") long userId);

    Long selectUserId(@Param("token") String token);

    int deleteByToken(@Param("token") String token);
}
