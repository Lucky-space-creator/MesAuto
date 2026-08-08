package com.lucky.mescore.modules.warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.warehouse.entity.*;
import com.lucky.mescore.modules.warehouse.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/api/inventory/log")
    public R<PageResponse<?>> inventoryLog(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(PageResponse.empty());
    }
}
