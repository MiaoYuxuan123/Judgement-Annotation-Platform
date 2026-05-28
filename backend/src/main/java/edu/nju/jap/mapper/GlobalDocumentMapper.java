package edu.nju.jap.mapper;

import edu.nju.jap.model.po.GlobalDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GlobalDocumentMapper {
    GlobalDocument selectById(@Param("id") long id);

    List<GlobalDocument> selectAll(@Param("keyword") String keyword);

    int insert(GlobalDocument doc);

    int deleteById(@Param("id") long id);
}
