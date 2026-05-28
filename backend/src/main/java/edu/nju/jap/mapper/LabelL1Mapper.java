package edu.nju.jap.mapper;

import edu.nju.jap.model.po.LabelL1;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LabelL1Mapper {
    List<LabelL1> selectByVersionId(@Param("versionId") int versionId);

    int deleteByVersionId(@Param("versionId") int versionId);

    int insert(LabelL1 label);
}
