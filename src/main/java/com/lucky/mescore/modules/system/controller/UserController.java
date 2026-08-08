package com.lucky.mescore.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.system.dto.UserDTO;
import com.lucky.mescore.modules.system.dto.UserQueryDTO;
import com.lucky.mescore.modules.system.entity.User;
import com.lucky.mescore.modules.system.entity.UserRole;
import com.lucky.mescore.modules.system.mapper.UserRoleMapper;
import com.lucky.mescore.modules.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRoleMapper userRoleMapper;

    @PostMapping("/page")
    public R<PageResponse<User>> page(@RequestBody PageRequest<UserQueryDTO> request) {
        UserQueryDTO condition = request.getCondition();
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getUsername() != null, User::getUsername, condition.getUsername())
              .like(condition.getRealName() != null, User::getRealName, condition.getRealName())
              .eq(condition.getStatus() != null, User::getStatus, condition.getStatus());
        }
        qw.orderByDesc(User::getCreateTime);
        Page<User> page = userService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return R.ok(user);
    }

    @PostMapping
    public R<Void> create(@RequestBody UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        userService.save(user);
        if (dto.getRoleIds() != null) {
            userService.assignRoles(user.getId(), dto.getRoleIds());
        }
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        User user = new User();
        user.setId(id);
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(dto.getStatus());
        userService.updateById(user);
        if (dto.getRoleIds() != null) {
            userService.assignRoles(id, dto.getRoleIds());
        }
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @GetMapping("/{id}/roles")
    public R<List<Long>> getUserRoleIds(@PathVariable Long id) {
        return R.ok(userRoleMapper.selectRoleIdsByUserId(id));
    }
}
