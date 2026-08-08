package com.lucky.mescore.modules.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.entity.MaterialCategory;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.mapper.MaterialCategoryMapper;
import com.lucky.mescore.modules.material.mapper.UnitMapper;
import com.lucky.mescore.modules.warehouse.entity.StorageLocation;
import com.lucky.mescore.modules.warehouse.entity.Warehouse;
import com.lucky.mescore.modules.warehouse.mapper.StorageLocationMapper;
import com.lucky.mescore.modules.warehouse.mapper.WarehouseMapper;
import com.lucky.mescore.modules.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final UnitMapper unitMapper;
    private final MaterialCategoryMapper categoryMapper;
    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper locationMapper;

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
        enrich(page.getRecords());
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
        enrich(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Material> getById(@PathVariable Long id) {
        Material material = materialService.getById(id);
        enrich(List.of(material));
        return R.ok(material);
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

    /** 补充前端展示用的关联名称（单位/分类），不影响持久化 */
    private void enrich(List<Material> materials) {
        if (materials == null || materials.isEmpty()) return;
        Set<Long> unitIds = materials.stream().map(Material::getPrimaryUnitId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> categoryIds = materials.stream().map(Material::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = materials.stream().map(Material::getDefaultWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locationIds = materials.stream().map(Material::getDefaultLocationId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Unit> unitMap = unitIds.isEmpty() ? Map.of() :
                unitMapper.selectBatchIds(unitIds).stream()
                        .collect(Collectors.toMap(Unit::getId, Function.identity()));
        Map<Long, MaterialCategory> categoryMap = categoryIds.isEmpty() ? Map.of() :
                categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(MaterialCategory::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of() :
                warehouseMapper.selectBatchIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, StorageLocation> locationMap = locationIds.isEmpty() ? Map.of() :
                locationMapper.selectBatchIds(locationIds).stream()
                        .collect(Collectors.toMap(StorageLocation::getId, Function.identity()));
        for (Material m : materials) {
            if (m.getPrimaryUnitId() != null && unitMap.containsKey(m.getPrimaryUnitId())) {
                m.setUnitName(unitMap.get(m.getPrimaryUnitId()).getUnitName());
            }
            if (m.getCategoryId() != null && categoryMap.containsKey(m.getCategoryId())) {
                m.setCategoryName(categoryMap.get(m.getCategoryId()).getCategoryName());
            }
            if (m.getDefaultWarehouseId() != null && warehouseMap.containsKey(m.getDefaultWarehouseId())) {
                m.setWarehouseName(warehouseMap.get(m.getDefaultWarehouseId()).getWarehouseName());
            }
            if (m.getDefaultLocationId() != null && locationMap.containsKey(m.getDefaultLocationId())) {
                m.setLocationCode(locationMap.get(m.getDefaultLocationId()).getLocationCode());
            }
        }
    }
}
