package edu.nju.jap.mapper;

import edu.nju.jap.model.po.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper {
    SysUser selectById(@Param("id") long id);

    SysUser selectByUsername(@Param("username") String username);

    List<SysUser> selectAll();

    int insert(SysUser user);

    int update(SysUser user);

    int deleteById(@Param("id") long id);

    int updateOnline(@Param("id") long id);

    int updateOffline(@Param("id") long id);

    int updateOfflineByTimeout();
}
