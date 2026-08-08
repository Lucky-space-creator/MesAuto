package com.lucky.mescore.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.system.entity.User;

import java.util.Set;

public interface UserService extends IService<User> {

    Set<String> getUserRoles(Long userId);

    Set<String> getUserPermissions(Long userId);

    void assignRoles(Long userId, java.util.List<Long> roleIds);
}
