package com.lucky.mescore.modules.schedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import com.lucky.mescore.modules.schedule.entity.*;
import com.lucky.mescore.modules.schedule.mapper.*;
import com.lucky.mescore.modules.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final WorkCenterMapper workCenterMapper;
    private final WorkstationMapper workstationMapper;
    private final ProductionPlanMapper planMapper;
    private final ProductionTaskMapper taskMapper;
    private final TaskReportMapper reportMapper;
    private final OrderMapper orderMapper;
    private final MaterialMapper materialMapper;

    @PostMapping("/plan")
    public R<ProductionPlan> generatePlan(@RequestParam Long orderId) {
        return R.ok(scheduleService.generatePlan(orderId));
    }

    @PostMapping("/plan/page")
    public R<PageResponse<ProductionPlan>> planPage(@RequestBody PageRequest<Void> request) {
        Page<ProductionPlan> page = planMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<ProductionPlan>().orderByDesc(ProductionPlan::getCreateTime));
        enrichPlan(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping("/task/page")
    public R<PageResponse<ProductionTask>> taskPage(@RequestBody PageRequest<ProductionTask> request) {
        ProductionTask condition = request.getCondition();
        LambdaQueryWrapper<ProductionTask> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.eq(condition.getTaskStatus() != null, ProductionTask::getTaskStatus, condition.getTaskStatus())
              .eq(condition.getWorkstationId() != null, ProductionTask::getWorkstationId, condition.getWorkstationId());
        }
        qw.orderByDesc(ProductionTask::getCreateTime);
        Page<ProductionTask> page = taskMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), qw);
        enrichTask(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping("/task/{id}/start")
    public R<Void> startTask(@PathVariable Long id, @RequestParam String operator) {
        scheduleService.startTask(id, operator);
        return R.ok();
    }

    @PostMapping("/task/{id}/report")
    public R<Void> reportTask(@PathVariable Long id,
                              @RequestParam BigDecimal qty,
                              @RequestParam(required = false) BigDecimal defectiveQty,
                              @RequestParam String operator) {
        scheduleService.reportTask(id, qty, defectiveQty, operator);
        return R.ok();
    }

    @PostMapping("/task/{id}/complete")
    public R<Void> completeTask(@PathVariable Long id, @RequestParam String operator) {
        scheduleService.completeTask(id, operator);
        return R.ok();
    }

    @PostMapping("/task/{id}/pause")
    public R<Void> pauseTask(@PathVariable Long id, @RequestParam String operator) {
        scheduleService.pauseTask(id, operator);
        return R.ok();
    }

    @PostMapping("/task/{id}/resume")
    public R<Void> resumeTask(@PathVariable Long id, @RequestParam String operator) {
        scheduleService.resumeTask(id, operator);
        return R.ok();
    }

    @GetMapping("/task/{id}/reports")
    public R<List<TaskReport>> taskReports(@PathVariable Long id) {
        return R.ok(reportMapper.selectList(
                new LambdaQueryWrapper<TaskReport>().eq(TaskReport::getTaskId, id).orderByDesc(TaskReport::getCreateTime)));
    }

    @GetMapping("/gantt")
    public R<List<ProductionTask>> gantt(@RequestParam Long orderId) {
        return R.ok(taskMapper.selectList(
                new LambdaQueryWrapper<ProductionTask>().eq(ProductionTask::getOrderId, orderId)));
    }

    // ===== 工作中心/工位 =====

    @GetMapping("/workstation/all")
    public R<List<Workstation>> allStations() {
        return R.ok(workstationMapper.selectList(null));
    }

    @PostMapping("/workstation")
    public R<Void> createStation(@RequestBody Workstation station) {
        workstationMapper.insert(station);
        return R.ok();
    }

    @PutMapping("/workstation/{id}")
    public R<Void> updateStation(@PathVariable Long id, @RequestBody Workstation station) {
        station.setId(id);
        workstationMapper.updateById(station);
        return R.ok();
    }

    @GetMapping("/workcenter/all")
    public R<List<WorkCenter>> allCenters() {
        return R.ok(workCenterMapper.selectList(null));
    }

    @PostMapping("/workcenter")
    public R<Void> createCenter(@RequestBody WorkCenter center) {
        workCenterMapper.insert(center);
        return R.ok();
    }

    @PutMapping("/workcenter/{id}")
    public R<Void> updateCenter(@PathVariable Long id, @RequestBody WorkCenter center) {
        center.setId(id);
        workCenterMapper.updateById(center);
        return R.ok();
    }

    /** 补充计划列表关联订单号 */
    private void enrichPlan(List<ProductionPlan> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> orderIds = list.stream().map(ProductionPlan::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderIds.isEmpty() ? Map.of() :
                orderMapper.selectBatchIds(orderIds).stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        for (ProductionPlan r : list) {
            if (r.getOrderId() != null && orderMap.containsKey(r.getOrderId()))
                r.setOrderNo(orderMap.get(r.getOrderId()).getOrderNo());
        }
    }

    /** 补充任务列表关联名称（物料/工位/订单） */
    private void enrichTask(List<ProductionTask> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> materialIds = list.stream().map(ProductionTask::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> stationIds = list.stream().map(ProductionTask::getWorkstationId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> orderIds = list.stream().map(ProductionTask::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() :
                materialMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Workstation> stationMap = stationIds.isEmpty() ? Map.of() :
                workstationMapper.selectBatchIds(stationIds).stream().collect(Collectors.toMap(Workstation::getId, Function.identity()));
        Map<Long, Order> orderMap = orderIds.isEmpty() ? Map.of() :
                orderMapper.selectBatchIds(orderIds).stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        for (ProductionTask r : list) {
            if (r.getMaterialId() != null && materialMap.containsKey(r.getMaterialId()))
                r.setMaterialName(materialMap.get(r.getMaterialId()).getMaterialName());
            if (r.getWorkstationId() != null && stationMap.containsKey(r.getWorkstationId()))
                r.setWorkstationName(stationMap.get(r.getWorkstationId()).getStationName());
            if (r.getOrderId() != null && orderMap.containsKey(r.getOrderId()))
                r.setOrderNo(orderMap.get(r.getOrderId()).getOrderNo());
        }
    }
}
