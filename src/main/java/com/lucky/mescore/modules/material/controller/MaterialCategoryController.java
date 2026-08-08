package com.lucky.mescore.modules.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.MaterialCategory;
import com.lucky.mescore.modules.material.service.MaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/material/category")
@RequiredArgsConstructor
public class MaterialCategoryController {

    private final MaterialCategoryService categoryService;

    @GetMapping("/tree")
    public R<List<MaterialCategory>> tree() {
        List<MaterialCategory> all = categoryService.list();
        return R.ok(buildTree(all, 0L));
    }

    @GetMapping("/all")
    public R<List<MaterialCategory>> all() {
        return R.ok(categoryService.list(new LambdaQueryWrapper<MaterialCategory>().eq(MaterialCategory::getStatus, 1)));
    }

    @PostMapping
    public R<Void> create(@RequestBody MaterialCategory category) {
        categoryService.save(category);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody MaterialCategory category) {
        category.setId(id);
        categoryService.updateById(category);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return R.ok();
    }

    private List<MaterialCategory> buildTree(List<MaterialCategory> all, Long parentId) {
        return all.stream()
                .filter(c -> parentId.equals(c.getParentId() != null ? c.getParentId() : 0L))
                .peek(c -> {
                    List<MaterialCategory> children = buildTree(all, c.getId());
                })
                .collect(Collectors.toList());
    }
}
