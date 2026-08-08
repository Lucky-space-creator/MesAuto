package com.lucky.mescore.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.system.dto.RoleDTO;
import com.lucky.mescore.modules.system.entity.Role;
import com.lucky.mescore.modules.system.entity.RolePermission;
import com.lucky.mescore.modules.system.mapper.RolePermissionMapper;
import com.lucky.mescore.modules.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final RolePermissionMapper rolePermissionMapper;

    @PostMapping("/page")
    public R<PageResponse<Role>> page(@RequestBody PageRequest<Void> request) {
        LambdaQueryWrapper<Role> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(Role::getCreateTime);
        Page<Role> page = roleService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/all")
    public R<List<Role>> all() {
        return R.ok(roleService.list(new LambdaQueryWrapper<Role>().eq(Role::getStatus, 1)));
    }

    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody RoleDTO dto) {
        Role role = new Role();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        roleService.save(role);
        if (dto.getPermissionIds() != null) {
            roleService.assignPermissions(role.getId(), dto.getPermissionIds());
        }
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody RoleDTO dto) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(dto.getRoleName());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus());
        roleService.updateById(role);
        if (dto.getPermissionIds() != null) {
            roleService.assignPermissions(id, dto.getPermissionIds());
        }
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return R.ok();
    }

    @GetMapping("/{id}/permissions")
    public R<List<Long>> getPermissionIds(@PathVariable Long id) {
        List<RolePermission> list = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        return R.ok(list.stream().map(RolePermission::getPermissionId).collect(Collectors.toList()));
    }
}
