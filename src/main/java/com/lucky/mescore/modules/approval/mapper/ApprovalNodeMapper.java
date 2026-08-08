package com.lucky.mescore.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.approval.entity.ApprovalNode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApprovalNodeMapper extends BaseMapper<ApprovalNode> {

    @Select("SELECT * FROM mes_approval_node WHERE template_id = #{templateId} ORDER BY node_seq")
    List<ApprovalNode> selectByTemplateId(Long templateId);

    @Delete("DELETE FROM mes_approval_node WHERE template_id = #{templateId}")
    void deleteByTemplateId(Long templateId);
}
