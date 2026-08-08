package com.lucky.mescore.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Bom;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.mapper.BomMapper;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.entity.OrderItem;
import com.lucky.mescore.modules.order.service.OrderService;
import com.lucky.mescore.modules.process.entity.ProcessRoute;
import com.lucky.mescore.modules.process.mapper.ProcessRouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MaterialMapper materialMapper;
    private final BomMapper bomMapper;
    private final ProcessRouteMapper processRouteMapper;

    @PostMapping("/page")
    public R<PageResponse<Order>> page(@RequestBody PageRequest<Order> request) {
        Order condition = request.getCondition();
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getOrderNo() != null, Order::getOrderNo, condition.getOrderNo())
              .eq(condition.getOrderStatus() != null, Order::getOrderStatus, condition.getOrderStatus())
              .eq(condition.getMaterialId() != null, Order::getMaterialId, condition.getMaterialId())
              .eq(condition.getSourceType() != null, Order::getSourceType, condition.getSourceType())
              .ge(condition.getPlanStartDate() != null, Order::getPlanStartDate, condition.getPlanStartDate())
              .le(condition.getPlanEndDate() != null, Order::getPlanEndDate, condition.getPlanEndDate());
        }
        qw.orderByDesc(Order::getCreateTime);
        Page<Order> page = orderService.page(new Page<>(request.getPageNum(), request.getPageSize()), qw);
        enrich(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Order> getById(@PathVariable Long id) {
        Order order = orderService.getById(id);
        enrich(List.of(order));
        return R.ok(order);
    }

    /** 补充前端展示用的关联名称字段（物料/BOM/工艺路线），不影响持久化 */
    private void enrich(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        Set<Long> materialIds = orders.stream().map(Order::getMaterialId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Set<Long> bomIds = orders.stream().map(Order::getBomId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Set<Long> routeIds = orders.stream().map(Order::getRouteId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() :
            materialMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Bom> bomMap = bomIds.isEmpty() ? Map.of() :
            bomMapper.selectBatchIds(bomIds).stream().collect(Collectors.toMap(Bom::getId, Function.identity()));
        Map<Long, ProcessRoute> routeMap = routeIds.isEmpty() ? Map.of() :
            processRouteMapper.selectBatchIds(routeIds).stream().collect(Collectors.toMap(ProcessRoute::getId, Function.identity()));
        for (Order o : orders) {
            if (o.getMaterialId() != null && materialMap.containsKey(o.getMaterialId())) {
                o.setMaterialName(materialMap.get(o.getMaterialId()).getMaterialName());
            }
            if (o.getBomId() != null && bomMap.containsKey(o.getBomId())) {
                o.setBomName(bomMap.get(o.getBomId()).getBomName());
            }
            if (o.getRouteId() != null && routeMap.containsKey(o.getRouteId())) {
                o.setRouteName(routeMap.get(o.getRouteId()).getRouteName());
            }
        }
    }

    @GetMapping("/{id}/items")
    public R<List<OrderItem>> items(@PathVariable Long id) {
        return R.ok(orderService.getOrderItems(id));
    }

    @PostMapping
    public R<Order> create(@RequestBody Order order) {
        orderService.saveOrderWithItems(order, null);
        return R.ok(order);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        orderService.updateOrderWithItems(order, null);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        orderService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<Void> submitApproval(@PathVariable Long id, @RequestParam String applicant) {
        orderService.submitApproval(id, applicant);
        return R.ok();
    }

    @PostMapping("/{id}/release")
    public R<Void> release(@PathVariable Long id) {
        orderService.release(id);
        return R.ok();
    }

    @PostMapping("/{id}/start")
    public R<Void> startProduction(@PathVariable Long id) {
        orderService.startProduction(id);
        return R.ok();
    }

    /** 生产完工 → 待入库，补齐此前缺失的环节，否则订单无法走到已完成 */
    @PostMapping("/{id}/finish-production")
    public R<Void> finishProduction(@PathVariable Long id) {
        orderService.completeProduction(id);
        return R.ok();
    }

    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id) {
        orderService.complete(id);
        return R.ok();
    }

    /** 查询当前状态下允许编辑的字段，前端据此禁用不可编辑控件 */
    @GetMapping("/{id}/editable-fields")
    public R<List<String>> editableFields(@PathVariable Long id) {
        return R.ok(orderService.editableFields(id));
    }

    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id) {
        orderService.close(id);
        return R.ok();
    }

    @PostMapping("/{id}/items")
    public R<Void> saveItems(@PathVariable Long id, @RequestBody List<OrderItem> items) {
        Order order = new Order();
        order.setId(id);
        orderService.updateOrderWithItems(order, items);
        return R.ok();
    }
}
