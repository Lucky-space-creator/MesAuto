package com.lucky.mescore.modules.schedule.service;

import com.lucky.mescore.common.enums.OrderStatusEnum;
import com.lucky.mescore.common.enums.TaskStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import com.lucky.mescore.modules.process.entity.ProcessRoute;
import com.lucky.mescore.modules.process.entity.ProcessStep;
import com.lucky.mescore.modules.process.mapper.ProcessRouteMapper;
import com.lucky.mescore.modules.process.mapper.ProcessStepMapper;
import com.lucky.mescore.modules.schedule.entity.*;
import com.lucky.mescore.modules.schedule.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final OrderMapper orderMapper;
    private final ProcessRouteMapper routeMapper;
    private final ProcessStepMapper stepMapper;
    private final ProductionPlanMapper planMapper;
    private final ProductionTaskMapper taskMapper;
    private final TaskReportMapper reportMapper;
    private final WorkstationMapper workstationMapper;

    @Transactional(rollbackFor = Exception.class)
    public ProductionPlan generatePlan(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !OrderStatusEnum.RELEASED.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("仅已下达的订单可生成排程计划");
        }

        ProductionPlan plan = new ProductionPlan();
        plan.setPlanNo("MP" + LocalDate.now().toString().replace("-", "") + System.currentTimeMillis());
        plan.setOrderId(orderId);
        plan.setTotalQty(order.getPlannedQty());
        plan.setCompletedQty(BigDecimal.ZERO);
        plan.setPlanStatus("PLANNED");
        plan.setPlanDate(LocalDate.now());
        planMapper.insert(plan);

        Long routeId = order.getRouteId();
        List<ProcessStep> steps;
        if (routeId != null) {
            ProcessRoute route = routeMapper.selectById(routeId);
            if (route != null) {
                steps = stepMapper.selectByRouteId(routeId);
            } else {
                throw new BusinessException("工艺路线不存在");
            }
        } else {
            // 默认物料工艺路线
            ProcessRoute route = routeMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProcessRoute>()
                            .eq(ProcessRoute::getMaterialId, order.getMaterialId())
                            .eq(ProcessRoute::getStatus, "PUBLISHED")
                            .last("LIMIT 1")).stream().findFirst().orElse(null);
            if (route == null) {
                throw new BusinessException("未找到默认工艺路线");
            }
            steps = stepMapper.selectByRouteId(route.getId());
        }

        for (int i = 0; i < steps.size(); i++) {
            ProcessStep step = steps.get(i);
            List<Workstation> stations = workstationMapper.selectByCenterId(step.getWorkCenterId());
            if (stations.isEmpty()) {
                throw new BusinessException("工作中心 [" + step.getWorkCenterId() + "] 无可用工位");
            }
            ProductionTask task = new ProductionTask();
            task.setTaskNo("MT" + System.currentTimeMillis() + "_" + i);
            task.setPlanId(plan.getId());
            task.setOrderId(orderId);
            task.setMaterialId(order.getMaterialId());
            task.setStepId(step.getId());
            task.setWorkstationId(stations.get(0).getId());
            task.setPlannedQty(order.getPlannedQty());
            task.setActualQty(BigDecimal.ZERO);
            task.setUnitId(order.getUnitId());
            task.setTaskStatus(TaskStatusEnum.PENDING.getCode());
            task.setPriority(order.getPriority());
            task.setPlanStartTime(LocalDateTime.now());
            task.setPlanEndTime(LocalDateTime.now().plusDays(1));
            taskMapper.insert(task);
        }

        plan.setPlanStatus("IN_PROGRESS");
        planMapper.updateById(plan);

        order.setOrderStatus(OrderStatusEnum.IN_PRODUCTION.getCode());
        orderMapper.updateById(order);

        return plan;
    }

    @Transactional(rollbackFor = Exception.class)
    public void startTask(Long taskId, String operator) {
        ProductionTask task = taskMapper.selectById(taskId);
        if (task == null || !TaskStatusEnum.PENDING.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务不存在或无法开工");
        }
        task.setTaskStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        task.setActualStartTime(LocalDateTime.now());
        task.setAssignee(operator);
        taskMapper.updateById(task);

        TaskReport report = new TaskReport();
        report.setTaskId(taskId);
        report.setReportType("START");
        report.setOperator(operator);
        reportMapper.insert(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reportTask(Long taskId, BigDecimal qty, BigDecimal defectiveQty, String operator) {
        ProductionTask task = taskMapper.selectById(taskId);
        if (task == null || !TaskStatusEnum.IN_PROGRESS.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务状态不正确");
        }
        task.setActualQty(task.getActualQty().add(qty));
        if (defectiveQty != null) {
            task.setDefectiveQty((task.getDefectiveQty() != null ? task.getDefectiveQty() : BigDecimal.ZERO).add(defectiveQty));
        }
        if (task.getActualQty().compareTo(task.getPlannedQty()) >= 0) {
            task.setTaskStatus(TaskStatusEnum.COMPLETED.getCode());
            task.setActualEndTime(LocalDateTime.now());
        }
        taskMapper.updateById(task);

        TaskReport report = new TaskReport();
        report.setTaskId(taskId);
        report.setReportType("REPORT");
        report.setReportQty(qty);
        report.setDefectiveQty(defectiveQty);
        report.setOperator(operator);
        reportMapper.insert(report);

        ProductionPlan plan = planMapper.selectById(task.getPlanId());
        if (plan != null) {
            plan.setCompletedQty(plan.getCompletedQty().add(qty));
            if (TaskStatusEnum.COMPLETED.getCode().equals(task.getTaskStatus())) {
                plan.setPlanStatus("COMPLETED");
            }
            planMapper.updateById(plan);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void pauseTask(Long taskId, String operator) {
        ProductionTask task = taskMapper.selectById(taskId);
        if (task == null || !TaskStatusEnum.IN_PROGRESS.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("仅生产中任务可暂停");
        }
        task.setTaskStatus(TaskStatusEnum.PAUSED.getCode());
        taskMapper.updateById(task);

        TaskReport report = new TaskReport();
        report.setTaskId(taskId);
        report.setReportType("PAUSE");
        report.setOperator(operator);
        reportMapper.insert(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resumeTask(Long taskId, String operator) {
        ProductionTask task = taskMapper.selectById(taskId);
        if (task == null || !TaskStatusEnum.PAUSED.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("仅已暂停任务可恢复");
        }
        task.setTaskStatus(TaskStatusEnum.IN_PROGRESS.getCode());
        taskMapper.updateById(task);

        TaskReport report = new TaskReport();
        report.setTaskId(taskId);
        report.setReportType("RESUME");
        report.setOperator(operator);
        reportMapper.insert(report);
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, String operator) {
        ProductionTask task = taskMapper.selectById(taskId);
        if (task == null || !TaskStatusEnum.IN_PROGRESS.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("仅生产中任务可直接完成");
        }
        task.setTaskStatus(TaskStatusEnum.COMPLETED.getCode());
        task.setActualEndTime(LocalDateTime.now());
        taskMapper.updateById(task);

        TaskReport report = new TaskReport();
        report.setTaskId(taskId);
        report.setReportType("COMPLETE");
        report.setOperator(operator);
        reportMapper.insert(report);
    }
}
