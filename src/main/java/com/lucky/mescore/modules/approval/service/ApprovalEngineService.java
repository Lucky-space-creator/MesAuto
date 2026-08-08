package com.lucky.mescore.modules.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lucky.mescore.common.enums.ApprovalStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.approval.entity.*;
import com.lucky.mescore.modules.approval.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalEngineService {

    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalNodeMapper nodeMapper;
    private final ApprovalProcessMapper processMapper;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalRecordMapper recordMapper;

    @Transactional(rollbackFor = Exception.class)
    public ApprovalProcess submit(String bizType, Long bizId, String bizNo, String applicant) {
        ApprovalTemplate template = selectTemplate(bizType, null);
        if (template == null) {
            throw new BusinessException("未找到适用的审批模板");
        }
        List<ApprovalNode> nodes = nodeMapper.selectByTemplateId(template.getId());
        if (nodes.isEmpty()) {
            throw new BusinessException("审批模板未配置审批节点");
        }

        ApprovalProcess process = new ApprovalProcess();
        process.setTemplateId(template.getId());
        process.setBizType(bizType);
        process.setBizId(bizId);
        process.setBizNo(bizNo);
        process.setStatus(ApprovalStatusEnum.RUNNING.getCode());
        process.setApplicant(applicant);
        process.setStartTime(LocalDateTime.now());
        processMapper.insert(process);

        ApprovalNode firstNode = nodes.get(0);
        process.setCurrentNodeId(firstNode.getId());
        processMapper.updateById(process);

        createTask(process, firstNode, template.getId(), bizType, bizId);
        return process;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, String assignee, String comment) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        task.setStatus("COMPLETED");
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        ApprovalProcess process = processMapper.selectById(task.getProcessId());
        ApprovalNode currentNode = nodeMapper.selectById(task.getNodeId());

        recordAction(process.getId(), currentNode.getId(), currentNode.getNodeName(), assignee, "AGREE", comment);

        List<ApprovalNode> nodes = nodeMapper.selectByTemplateId(task.getTemplateId());
        ApprovalNode nextNode = findNextNode(nodes, currentNode.getNodeSeq());

        if (nextNode == null) {
            process.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            process.setEndTime(LocalDateTime.now());
            processMapper.updateById(process);
        } else {
            process.setCurrentNodeId(nextNode.getId());
            processMapper.updateById(process);
            createTask(process, nextNode, task.getTemplateId(), task.getBizType(), task.getBizId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, String assignee, String comment) {
        ApprovalTask task = taskMapper.selectById(taskId);
        if (task == null || !"PENDING".equals(task.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        task.setStatus("COMPLETED");
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        ApprovalProcess process = processMapper.selectById(task.getProcessId());
        process.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        process.setEndTime(LocalDateTime.now());
        processMapper.updateById(process);

        ApprovalNode node = nodeMapper.selectById(task.getNodeId());
        recordAction(process.getId(), node.getId(), node.getNodeName(), assignee, "REJECT", comment);
    }

    private ApprovalTemplate selectTemplate(String bizType, Object orderDTO) {
        List<ApprovalTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<ApprovalTemplate>()
                        .eq(ApprovalTemplate::getBizType, bizType)
                        .eq(ApprovalTemplate::getStatus, "PUBLISHED")
                        .orderByDesc(ApprovalTemplate::getPriority));

        if (templates.isEmpty()) return null;
        return templates.stream()
                .filter(t -> t.getIsDefault() == 1)
                .findFirst()
                .orElse(templates.get(0));
    }

    private ApprovalNode findNextNode(List<ApprovalNode> nodes, int currentSeq) {
        return nodes.stream()
                .filter(n -> n.getNodeSeq() > currentSeq)
                .findFirst()
                .orElse(null);
    }

    private void createTask(ApprovalProcess process, ApprovalNode node, Long templateId, String bizType, Long bizId) {
        ApprovalTask task = new ApprovalTask();
        task.setProcessId(process.getId());
        task.setTemplateId(templateId);
        task.setNodeId(node.getId());
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setAssignee(resolveAssignee(node));
        task.setStatus("PENDING");
        taskMapper.insert(task);
    }

    private String resolveAssignee(ApprovalNode node) {
        return switch (node.getAssigneeType()) {
            case "SPECIFIC" -> String.valueOf(node.getAssigneeId());
            case "ROLE" -> "role:" + node.getAssigneeId();
            case "DYNAMIC" -> "dynamic:" + node.getAssigneeExpr();
            default -> "sysadmin";
        };
    }

    private void recordAction(Long processId, Long nodeId, String nodeName, String assignee, String action, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setProcessId(processId);
        record.setNodeId(nodeId);
        record.setNodeName(nodeName);
        record.setAssignee(assignee);
        record.setAction(action);
        record.setComment(comment);
        recordMapper.insert(record);
    }
}
