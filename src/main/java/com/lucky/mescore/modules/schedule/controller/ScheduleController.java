package com.lucky.mescore.modules.schedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.schedule.entity.*;
import com.lucky.mescore.modules.schedule.mapper.*;
import com.lucky.mescore.modules.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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

    @PostMapping("/plan")
    public R<ProductionPlan> generatePlan(@RequestParam Long orderId) {
        return R.ok(scheduleService.generatePlan(orderId));
    }

    @GetMapping("/plan/page")
    public R<PageResponse<ProductionPlan>> planPage(@RequestBody PageRequest<Void> request) {
        Page<ProductionPlan> page = planMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<ProductionPlan>().orderByDesc(ProductionPlan::getCreateTime));
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/task/page")
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
}
