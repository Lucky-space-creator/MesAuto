package com.lucky.mescore.modules.warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.mapper.UnitMapper;
import com.lucky.mescore.modules.warehouse.entity.*;
import com.lucky.mescore.modules.warehouse.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final InboundOrderMapper inboundMapper;
    private final InboundItemMapper inboundItemMapper;
    private final OutboundOrderMapper outboundMapper;
    private final OutboundItemMapper outboundItemMapper;
    private final MaterialMapper materialMapper;
    private final UnitMapper unitMapper;

    @GetMapping("/api/warehouse/all")
    public R<List<Warehouse>> allWarehouses() {
        return R.ok(warehouseMapper.selectList(null));
    }

    @PostMapping("/api/warehouse")
    public R<Void> createWarehouse(@RequestBody Warehouse warehouse) {
        warehouseMapper.insert(warehouse);
        return R.ok();
    }

    @PutMapping("/api/warehouse/{id}")
    public R<Void> updateWarehouse(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        warehouseMapper.updateById(warehouse);
        return R.ok();
    }

    @GetMapping("/api/warehouse/{id}/locations")
    public R<List<StorageLocation>> locations(@PathVariable Long id) {
        return R.ok(locationMapper.selectList(
                new LambdaQueryWrapper<StorageLocation>().eq(StorageLocation::getWarehouseId, id)));
    }

    @PostMapping("/api/warehouse/location")
    public R<Void> createLocation(@RequestBody StorageLocation location) {
        locationMapper.insert(location);
        return R.ok();
    }

    @PutMapping("/api/warehouse/location/{id}")
    public R<Void> updateLocation(@PathVariable Long id, @RequestBody StorageLocation location) {
        location.setId(id);
        locationMapper.updateById(location);
        return R.ok();
    }

    @PostMapping("/api/inventory/page")
    public R<PageResponse<Inventory>> inventoryPage(@RequestBody PageRequest<Inventory> request) {
        Inventory condition = request.getCondition();
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.eq(condition.getMaterialId() != null, Inventory::getMaterialId, condition.getMaterialId())
              .eq(condition.getWarehouseId() != null, Inventory::getWarehouseId, condition.getWarehouseId())
              .eq(condition.getLocationId() != null, Inventory::getLocationId, condition.getLocationId());
        }
        Page<Inventory> page = inventoryMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), qw);
        enrichInventory(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping("/api/inbound")
    public R<Void> createInbound(@RequestBody InboundOrder order) {
        inboundMapper.insert(order);
        return R.ok();
    }

    @PutMapping("/api/inbound/{id}")
    public R<Void> updateInbound(@PathVariable Long id, @RequestBody InboundOrder order) {
        order.setId(id);
        inboundMapper.updateById(order);
        return R.ok();
    }

    @PostMapping("/api/inbound/page")
    public R<PageResponse<InboundOrder>> inboundPage(@RequestBody PageRequest<Void> request) {
        Page<InboundOrder> page = inboundMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<InboundOrder>().orderByDesc(InboundOrder::getCreateTime));
        enrichInbound(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping("/api/outbound")
    public R<Void> createOutbound(@RequestBody OutboundOrder order) {
        outboundMapper.insert(order);
        return R.ok();
    }

    @PutMapping("/api/outbound/{id}")
    public R<Void> updateOutbound(@PathVariable Long id, @RequestBody OutboundOrder order) {
        order.setId(id);
        outboundMapper.updateById(order);
        return R.ok();
    }

    @PostMapping("/api/outbound/page")
    public R<PageResponse<OutboundOrder>> outboundPage(@RequestBody PageRequest<Void> request) {
        Page<OutboundOrder> page = outboundMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<OutboundOrder>().orderByDesc(OutboundOrder::getCreateTime));
        enrichOutbound(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /** 补充库存列表的关联名称（物料/仓库/库位/单位），不影响持久化 */
    private void enrichInventory(List<Inventory> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> materialIds = list.stream().map(Inventory::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = list.stream().map(Inventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locationIds = list.stream().map(Inventory::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> unitIds = list.stream().map(Inventory::getUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() :
                materialMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        Map<Long, StorageLocation> locationMap = locationIds.isEmpty() ? Map.of() :
                locationMapper.selectBatchIds(locationIds).stream().collect(Collectors.toMap(StorageLocation::getId, Function.identity()));
        Map<Long, Unit> unitMap = unitIds.isEmpty() ? Map.of() :
                unitMapper.selectBatchIds(unitIds).stream().collect(Collectors.toMap(Unit::getId, Function.identity()));
        for (Inventory r : list) {
            if (r.getMaterialId() != null && materialMap.containsKey(r.getMaterialId()))
                r.setMaterialName(materialMap.get(r.getMaterialId()).getMaterialName());
            if (r.getWarehouseId() != null && warehouseMap.containsKey(r.getWarehouseId()))
                r.setWarehouseName(warehouseMap.get(r.getWarehouseId()).getWarehouseName());
            if (r.getLocationId() != null && locationMap.containsKey(r.getLocationId()))
                r.setLocationCode(locationMap.get(r.getLocationId()).getLocationCode());
            if (r.getUnitId() != null && unitMap.containsKey(r.getUnitId()))
                r.setUnitName(unitMap.get(r.getUnitId()).getUnitName());
        }
    }

    /** 补充入库单关联名称（仓库） */
    private void enrichInbound(List<InboundOrder> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> warehouseIds = list.stream().map(InboundOrder::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        for (InboundOrder r : list) {
            if (r.getWarehouseId() != null && warehouseMap.containsKey(r.getWarehouseId()))
                r.setWarehouseName(warehouseMap.get(r.getWarehouseId()).getWarehouseName());
        }
    }

    /** 补充出库单关联名称（仓库） */
    private void enrichOutbound(List<OutboundOrder> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> warehouseIds = list.stream().map(OutboundOrder::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of() :
                warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        for (OutboundOrder r : list) {
            if (r.getWarehouseId() != null && warehouseMap.containsKey(r.getWarehouseId()))
                r.setWarehouseName(warehouseMap.get(r.getWarehouseId()).getWarehouseName());
        }
    }
}
