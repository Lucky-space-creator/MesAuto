package com.lucky.mescore.modules.system.controller;

import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.system.entity.Permission;
import com.lucky.mescore.modules.system.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/tree")
    public R<List<Permission>> tree() {
        List<Permission> all = permissionService.list();
        return R.ok(buildTree(all, 0L));
    }

    @GetMapping("/all")
    public R<List<Permission>> all() {
        return R.ok(permissionService.list());
    }

    @PostMapping
    public R<Void> create(@RequestBody Permission perm) {
        permissionService.save(perm);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Permission perm) {
        perm.setId(id);
        permissionService.updateById(perm);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        permissionService.removeById(id);
        return R.ok();
    }

    private List<Permission> buildTree(List<Permission> all, Long parentId) {
        return all.stream()
                .filter(p -> (parentId == 0L && (p.getParentId() == null || p.getParentId() == 0))
                        || parentId.equals(p.getParentId()))
                .peek(p -> p.setChildren(buildTree(all, p.getId())))
                .sorted(java.util.Comparator.comparing(
                        Permission::getSort, java.util.Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }
}
