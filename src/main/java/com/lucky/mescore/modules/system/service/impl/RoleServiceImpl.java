package com.lucky.mescore.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.system.entity.Role;
import com.lucky.mescore.modules.system.entity.RolePermission;
import com.lucky.mescore.modules.system.mapper.RoleMapper;
import com.lucky.mescore.modules.system.mapper.RolePermissionMapper;
import com.lucky.mescore.modules.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        rolePermissionMapper.deleteByRoleId(roleId);
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermission> rps = permissionIds.stream().map(permId -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permId);
                return rp;
            }).collect(Collectors.toList());
            rps.forEach(rolePermissionMapper::insert);
        }
    }
}
