package com.lucky.mescore.modules.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.entity.OrderItem;
import com.lucky.mescore.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/page")
    public R<PageResponse<Order>> page(@RequestBody PageRequest<Order> request) {
        Order condition = request.getCondition();
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getOrderNo() != null, Order::getOrderNo, condition.getOrderNo())
              .eq(condition.getOrderStatus() != null, Order::getOrderStatus, condition.getOrderStatus())
              .eq(condition.getMaterialId() != null, Order::getMaterialId, condition.getMaterialId())
              .ge(condition.getPlanStartDate() != null, Order::getCreateTime, condition.getPlanStartDate())
              .le(condition.getPlanEndDate() != null, Order::getCreateTime, condition.getPlanEndDate());
        }
        qw.orderByDesc(Order::getCreateTime);
        Page<Order> page = orderService.page(new Page<>(request.getPageNum(), request.getPageSize()), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Order> getById(@PathVariable Long id) {
        return R.ok(orderService.getById(id));
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

    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable Long id) {
        orderService.complete(id);
        return R.ok();
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
