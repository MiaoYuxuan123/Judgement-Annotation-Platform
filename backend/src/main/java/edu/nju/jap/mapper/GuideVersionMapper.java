package edu.nju.jap.mapper;

import edu.nju.jap.model.po.GuideVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GuideVersionMapper {
    GuideVersion selectById(@Param("id") int id);

    GuideVersion selectActive();

    List<GuideVersion> selectAll();

    int insert(GuideVersion version);

    int update(GuideVersion version);

    int deleteById(@Param("id") int id);
}
