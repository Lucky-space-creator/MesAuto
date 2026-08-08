package com.lucky.mescore.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.system.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteByRoleId(Long roleId);
}
