package com.lucky.mescore.modules.process.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.process.entity.ProcessStep;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProcessStepMapper extends BaseMapper<ProcessStep> {

    @Select("SELECT * FROM mes_process_step WHERE route_id = #{routeId} ORDER BY step_seq")
    List<ProcessStep> selectByRouteId(Long routeId);

    @Delete("DELETE FROM mes_process_step WHERE route_id = #{routeId}")
    void deleteByRouteId(Long routeId);
}
