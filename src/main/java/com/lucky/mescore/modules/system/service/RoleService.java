package com.lucky.mescore.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.system.entity.Role;

public interface RoleService extends IService<Role> {

    void assignPermissions(Long roleId, java.util.List<Long> permissionIds);
}
