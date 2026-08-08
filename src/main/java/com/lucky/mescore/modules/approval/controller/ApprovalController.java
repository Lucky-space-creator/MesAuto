package com.lucky.mescore.modules.approval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.approval.entity.*;
import com.lucky.mescore.modules.approval.mapper.ApprovalProcessMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalRecordMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalTaskMapper;
import com.lucky.mescore.modules.approval.service.ApprovalEngineService;
import com.lucky.mescore.modules.approval.service.ApprovalTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalEngineService approvalEngine;
    private final ApprovalTemplateService templateService;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalProcessMapper processMapper;
    private final ApprovalRecordMapper recordMapper;

    @PostMapping("/api/approval/{bizType}/{bizId}")
    public R<Void> submit(@PathVariable String bizType, @PathVariable Long bizId,
                          @RequestParam String applicant) {
        approvalEngine.submit(bizType, bizId, bizId.toString(), applicant);
        return R.ok();
    }

    @GetMapping("/api/approval/todo")
    public R<List<ApprovalTask>> todoList(@RequestParam String assignee) {
        return R.ok(taskMapper.selectList(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getAssignee, assignee)
                .eq(ApprovalTask::getStatus, "PENDING")
                .orderByDesc(ApprovalTask::getCreateTime)));
    }

    @GetMapping("/api/approval/done")
    public R<List<ApprovalRecord>> doneList(@RequestParam String assignee) {
        return R.ok(recordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getAssignee, assignee)
                .orderByDesc(ApprovalRecord::getCreateTime)));
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
