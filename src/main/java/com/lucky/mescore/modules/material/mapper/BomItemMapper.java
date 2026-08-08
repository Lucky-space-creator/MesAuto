package com.lucky.mescore.modules.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.material.entity.BomItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BomItemMapper extends BaseMapper<BomItem> {

    @Delete("DELETE FROM mes_bom_item WHERE bom_id = #{bomId}")
    void deleteByBomId(Long bomId);
}
