package com.lucky.mescore.modules.system.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.system.entity.User;
import com.lucky.mescore.modules.system.entity.UserRole;
import com.lucky.mescore.modules.system.mapper.PermissionMapper;
import com.lucky.mescore.modules.system.mapper.UserMapper;
import com.lucky.mescore.modules.system.mapper.UserRoleMapper;
import com.lucky.mescore.modules.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public Set<String> getUserRoles(Long userId) {
        return new HashSet<>(userRoleMapper.selectRoleCodesByUserId(userId));
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        return new HashSet<>(permissionMapper.selectPermsByUserId(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRole> userRoles = roleIds.stream().map(roleId -> {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                return ur;
            }).collect(Collectors.toList());
            userRoles.forEach(userRoleMapper::insert);
        }
    }

    @Override
    public boolean save(User user) {
        user.setSalt(UUID.randomUUID().toString().replace("-", ""));
        user.setPassword(SecureUtil.sha256(user.getId() + user.getSalt() + user.getPassword()));
        return super.save(user);
    }
}
