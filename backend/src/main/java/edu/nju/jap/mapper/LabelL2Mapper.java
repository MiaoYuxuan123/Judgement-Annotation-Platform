package edu.nju.jap.mapper;

import edu.nju.jap.model.po.LabelL2;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LabelL2Mapper {
    List<LabelL2> selectByVersionId(@Param("versionId") int versionId);

    int deleteByVersionId(@Param("versionId") int versionId);

    int insert(LabelL2 label);
}
