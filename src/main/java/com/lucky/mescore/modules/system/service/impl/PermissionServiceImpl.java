package com.lucky.mescore.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.modules.system.entity.Permission;
import com.lucky.mescore.modules.system.mapper.PermissionMapper;
import com.lucky.mescore.modules.system.service.PermissionService;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {
}
