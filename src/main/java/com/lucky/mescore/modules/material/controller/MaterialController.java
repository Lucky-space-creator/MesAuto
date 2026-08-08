package com.lucky.mescore.modules.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/page")
    public R<PageResponse<Material>> page(@RequestBody PageRequest<Material> request) {
        Material condition = request.getCondition();
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getMaterialCode() != null, Material::getMaterialCode, condition.getMaterialCode())
              .like(condition.getMaterialName() != null, Material::getMaterialName, condition.getMaterialName())
              .eq(condition.getCategoryId() != null, Material::getCategoryId, condition.getCategoryId())
              .eq(condition.getStatus() != null, Material::getStatus, condition.getStatus());
        }
        qw.orderByDesc(Material::getCreateTime);
        Page<Material> page = materialService.page(
                new Page<>(request.getPageNum(), request.getPageSize()), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/search")
    public R<PageResponse<Material>> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Material::getMaterialCode, keyword)
                    .or().like(Material::getMaterialName, keyword)
                    .or().like(Material::getMaterialSpec, keyword));
        }
        qw.eq(Material::getStatus, 1).orderByDesc(Material::getCreateTime);
        Page<Material> page = materialService.page(new Page<>(pageNum, pageSize), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Material> getById(@PathVariable Long id) {
        return R.ok(materialService.getById(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Material material) {
        materialService.save(material);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Material material) {
        material.setId(id);
        materialService.updateById(material);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        materialService.removeById(id);
        return R.ok();
    }
}
