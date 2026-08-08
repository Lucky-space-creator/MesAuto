package com.lucky.mescore.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.schedule.entity.Workstation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkstationMapper extends BaseMapper<Workstation> {

    @Select("SELECT * FROM mes_workstation WHERE work_center_id = #{centerId} AND status != 'DISABLED' AND deleted = 0")
    List<Workstation> selectByCenterId(Long centerId);
}
