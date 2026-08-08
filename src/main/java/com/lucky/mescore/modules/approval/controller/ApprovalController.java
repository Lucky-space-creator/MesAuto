package com.lucky.mescore.modules.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.approval.entity.*;
import com.lucky.mescore.modules.approval.mapper.ApprovalNodeMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalProcessMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalRecordMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalTaskMapper;
import com.lucky.mescore.modules.approval.service.ApprovalEngineService;
import com.lucky.mescore.modules.approval.service.ApprovalTemplateService;
import com.lucky.mescore.common.util.JwtUtil;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import com.lucky.mescore.modules.system.mapper.UserRoleMapper;
import io.jsonwebtoken.Claims;
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
public class ApprovalController {

    private final ApprovalEngineService approvalEngine;
    private final ApprovalTemplateService templateService;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalProcessMapper processMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApprovalNodeMapper nodeMapper;
    private final OrderMapper orderMapper;
    private final JwtUtil jwtUtil;
    private final UserRoleMapper userRoleMapper;

    @PostMapping("/api/approval/{bizType}/{bizId}")
    public R<Void> submit(@PathVariable String bizType, @PathVariable Long bizId,
                          @RequestParam String applicant) {
        approvalEngine.submit(bizType, bizId, bizId.toString(), applicant);
        return R.ok();
    }

    @GetMapping("/api/approval/todo")
    public R<List<ApprovalTask>> todoList(@RequestParam String assignee) {
        List<ApprovalTask> list = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getAssignee, assignee)
                .eq(ApprovalTask::getStatus, "PENDING")
                .orderByDesc(ApprovalTask::getCreateTime));
        enrichTasks(list);
        return R.ok(list);
    }

    /**
     * 我的待办：根据当前登录用户的账号及其所属角色解析待办任务。
     * 审批任务的 assignee 可能为「用户名」或「role:角色ID」，本接口自动合并查询。
     */
    @GetMapping("/api/approval/todo/mine")
    public R<List<ApprovalTask>> myTodoList(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return R.ok(java.util.Collections.emptyList());
        }
        // 兼容前端直接传 token 与标准 Bearer 前缀两种写法
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Long userId = jwtUtil.getUserId(token);
        Claims claims = jwtUtil.parseToken(token);
        String username = claims != null ? claims.get("username", String.class) : null;

        java.util.List<String> assignees = new java.util.ArrayList<>();
        boolean isAdmin = false;
        if (username != null) assignees.add(username);
        if (userId != null) {
            userRoleMapper.selectRoleIdsByUserId(userId).forEach(rid -> assignees.add("role:" + rid));
            isAdmin = userRoleMapper.selectRoleCodesByUserId(userId).contains("admin");
        }
        // 超管（admin 角色）可查看全部待办
        if (isAdmin) {
            List<ApprovalTask> list = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                    .eq(ApprovalTask::getStatus, "PENDING")
                    .orderByDesc(ApprovalTask::getCreateTime));
            enrichTasks(list);
            return R.ok(list);
        }
        if (assignees.isEmpty()) {
            return R.ok(java.util.Collections.emptyList());
        }
        List<ApprovalTask> list = taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .in(ApprovalTask::getAssignee, assignees)
                .eq(ApprovalTask::getStatus, "PENDING")
                .orderByDesc(ApprovalTask::getCreateTime));
        enrichTasks(list);
        return R.ok(list);
    }

    @GetMapping("/api/approval/done")
    public R<List<ApprovalRecord>> doneList(@RequestParam String assignee) {
        return R.ok(recordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAssignee, assignee)
                .orderByDesc(ApprovalRecord::getCreateTime)));
    }

    /** 补充待办列表的展示字段：当前节点名称、业务单号（订单号） */
    private void enrichTasks(List<ApprovalTask> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> nodeIds = list.stream().map(ApprovalTask::getNodeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> bizIds = list.stream().filter(t -> "ORDER".equals(t.getBizType()))
                .map(ApprovalTask::getBizId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> nodeNameMap = nodeIds.isEmpty() ? Map.of() :
                nodeMapper.selectBatchIds(nodeIds).stream()
                        .collect(Collectors.toMap(ApprovalNode::getId, ApprovalNode::getNodeName, (a, b) -> a));
        Map<Long, Order> orderMap = bizIds.isEmpty() ? Map.of() :
                orderMapper.selectBatchIds(bizIds).stream().collect(Collectors.toMap(Order::getId, Function.identity()));
        for (ApprovalTask t : list) {
            if (t.getNodeId() != null && nodeNameMap.containsKey(t.getNodeId()))
                t.setNodeName(nodeNameMap.get(t.getNodeId()));
            if ("ORDER".equals(t.getBizType()) && t.getBizId() != null && orderMap.containsKey(t.getBizId()))
                t.setBizNo(orderMap.get(t.getBizId()).getOrderNo());
        }
    }

    @PostMapping("/api/approval/{taskId}/approve")
    public R<Void> approve(@PathVariable Long taskId,
                           @RequestParam String assignee,
                           @RequestParam(required = false) String comment) {
        approvalEngine.approve(taskId, assignee, comment);
        return R.ok();
    }

    @PostMapping("/api/approval/{taskId}/reject")
    public R<Void> reject(@PathVariable Long taskId,
                          @RequestParam String assignee,
                          @RequestParam(required = false) String comment) {
        approvalEngine.reject(taskId, assignee, comment);
        return R.ok();
    }

    // ===== 审批模板管理 =====

    @PostMapping("/api/approval-template/page")
    public R<PageResponse<ApprovalTemplate>> templatePage(@RequestBody PageRequest<Void> request) {
        Page<ApprovalTemplate> page = templateService.page(
                new Page<>(request.getPageNum(), request.getPageSize()),
                new LambdaQueryWrapper<ApprovalTemplate>().orderByDesc(ApprovalTemplate::getCreateTime));
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/api/approval-template/{id}")
    public R<ApprovalTemplate> getTemplate(@PathVariable Long id) {
        return R.ok(templateService.getById(id));
    }

    @GetMapping("/api/approval-template/{id}/nodes")
    public R<List<ApprovalNode>> templateNodes(@PathVariable Long id) {
        return R.ok(templateService.getNodes(id));
    }

    @PostMapping("/api/approval-template")
    public R<ApprovalTemplate> createTemplate(@RequestBody ApprovalTemplate template) {
        templateService.saveTemplateWithNodes(template, null);
        return R.ok(template);
    }

    @PutMapping("/api/approval-template/{id}")
    public R<Void> updateTemplate(@PathVariable Long id, @RequestBody ApprovalTemplate template) {
        template.setId(id);
        templateService.updateById(template);
        return R.ok();
    }

    @DeleteMapping("/api/approval-template/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        templateService.removeById(id);
        return R.ok();
    }

    @PostMapping("/api/approval-template/{id}/publish")
    public R<Void> publishTemplate(@PathVariable Long id) {
        templateService.publish(id);
        return R.ok();
    }

    @PostMapping("/api/approval-template/{id}/toggle")
    public R<Void> toggleTemplate(@PathVariable Long id) {
        ApprovalTemplate template = templateService.getById(id);
        template.setStatus("PUBLISHED".equals(template.getStatus()) ? "DISABLED" : "PUBLISHED");
        templateService.updateById(template);
        return R.ok();
    }

    @PostMapping("/api/approval-template/{id}/nodes")
    public R<Void> saveTemplateNodes(@PathVariable Long id, @RequestBody List<ApprovalNode> nodes) {
        ApprovalTemplate template = new ApprovalTemplate();
        template.setId(id);
        templateService.updateTemplateWithNodes(template, nodes);
        return R.ok();
    }

    @GetMapping("/api/approval/route/preview")
    public R<List<ApprovalNode>> routePreview(@RequestParam String bizType) {
        ApprovalTemplate template = templateService.list(
                new LambdaQueryWrapper<ApprovalTemplate>()
                        .eq(ApprovalTemplate::getBizType, bizType)
                        .eq(ApprovalTemplate::getStatus, "PUBLISHED")
                        .eq(ApprovalTemplate::getIsDefault, 1)
                        .last("LIMIT 1")).stream().findFirst().orElse(null);
        if (template == null) return R.fail("未找到匹配模板");
        return R.ok(templateService.getNodes(template.getId()));
    }
}
