package com.lucky.mescore.modules.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lucky.mescore.common.enums.ApprovalStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.approval.engine.BpmnXmlGenerator;
import com.lucky.mescore.modules.approval.entity.*;
import com.lucky.mescore.modules.approval.event.ApprovalFinishedEvent;
import com.lucky.mescore.modules.approval.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activiti 驱动的审批引擎服务。
 * <p>
 * 审批模板发布时动态生成 BPMN 2.0 XML 并部署到 Activiti，
 * 提交审批时启动流程实例，审批通过/驳回时驱动 Activiti 任务节点。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalEngineService {

    private final ProcessEngine processEngine;
    private final ApprovalTemplateMapper templateMapper;
    private final ApprovalNodeMapper nodeMapper;
    private final ApprovalProcessMapper processMapper;
    private final ApprovalTaskMapper taskMapper;
    private final ApprovalRecordMapper recordMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== 模板发布（部署 BPMN） ====================

    /**
     * 将审批模板发布到 Activiti：生成 BPMN XML → 部署流程定义。
     * 如果已有同 key 的旧版流程定义，Activiti 会自动版本管理。
     */
    public void deployTemplate(ApprovalTemplate template) {
        List<ApprovalNode> nodes = nodeMapper.selectByTemplateId(template.getId());
        if (nodes.isEmpty()) {
            throw new BusinessException("审批模板 [" + template.getTemplateName() + "] 未配置审批节点");
        }
        String bpmnXml = BpmnXmlGenerator.generate(template, nodes);
        String resourceName = "processes/" + template.getTemplateCode() + ".bpmn20.xml";

        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeployment()
                .name(template.getTemplateName())
                .addString(resourceName, bpmnXml)
                .deploy();
        log.info("审批模板 [{}] 已部署到 Activiti", template.getTemplateCode());
    }

    // ==================== 提交审批 ====================

    @Transactional(rollbackFor = Exception.class)
    public ApprovalProcess submit(String bizType, Long bizId, String bizNo, String applicant) {
        // 1. 匹配审批模板
        ApprovalTemplate template = selectTemplate(bizType);
        if (template == null) {
            throw new BusinessException("未找到适用的审批模板，请先配置并发布审批模板");
        }

        List<ApprovalNode> nodes = nodeMapper.selectByTemplateId(template.getId());
        if (nodes.isEmpty()) {
            throw new BusinessException("审批模板未配置审批节点");
        }

        // 2. 确保 BPMN 已部署（检查流程定义是否存在）
        String processDefKey = "process_" + template.getTemplateCode();
        RuntimeService runtimeService = processEngine.getRuntimeService();
        RepositoryService repositoryService = processEngine.getRepositoryService();

        ProcessDefinition procDef = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefKey)
                .latestVersion()
                .singleResult();

        if (procDef == null) {
            // 首次提交时自动部署
            deployTemplate(template);
            procDef = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(processDefKey)
                    .latestVersion()
                    .singleResult();
        }

        // 3. 构建流程变量：为每个节点填充审批人信息
        Map<String, Object> variables = new HashMap<>();
        variables.put("templateId", template.getId());
        variables.put("bizType", bizType);
        variables.put("bizId", bizId);
        variables.put("bizNo", bizNo);
        variables.put("applicant", applicant);

        for (ApprovalNode node : nodes) {
            String assignee = resolveAssignee(node);
            variables.put("assignee_" + node.getId(), assignee);
            // candidateGroups 用于 ROLE 类型
            if ("ROLE".equals(node.getAssigneeType())) {
                variables.put("candidateGroups_" + node.getId(), "role_" + node.getAssigneeId());
            } else {
                variables.put("candidateGroups_" + node.getId(), "");
            }
        }

        // 4. 启动 Activiti 流程实例
        ProcessInstance procInst = runtimeService
                .startProcessInstanceByKey(processDefKey, variables);

        // 5. 写 mes_approval_process
        ApprovalProcess process = new ApprovalProcess();
        process.setTemplateId(template.getId());
        process.setBizType(bizType);
        process.setBizId(bizId);
        process.setBizNo(bizNo);
        process.setStatus(ApprovalStatusEnum.RUNNING.getCode());
        process.setApplicant(applicant);
        process.setProcInstId(procInst.getId());
        process.setStartTime(LocalDateTime.now());

        // 当前节点取模板第一个节点
        process.setCurrentNodeId(nodes.get(0).getId());

        processMapper.insert(process);

        // 6. 同步 Activiti 当前任务到 mes_approval_task
        syncTasks(process, procInst.getId());

        log.info("审批流程启动成功, processId={}, procInstId={}, template={}",
                process.getId(), procInst.getId(), template.getTemplateCode());

        return process;
    }

    // ==================== 审批通过 ====================

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long mesTaskId, String assignee, String comment) {
        ApprovalTask mesTask = taskMapper.selectById(mesTaskId);
        if (mesTask == null || !"PENDING".equals(mesTask.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        ApprovalProcess process = processMapper.selectById(mesTask.getProcessId());
        ApprovalNode currentNode = nodeMapper.selectById(mesTask.getNodeId());

        // 1. 记录审批动作
        recordAction(process.getId(), currentNode.getId(), currentNode.getNodeName(), assignee, "AGREE", comment);

        // 2. 完成 Activiti 任务（如果没有 activitiTaskId，则直接走本地逻辑降级）
        if (mesTask.getActivitiTaskId() != null) {
            completeActivitiTask(mesTask.getActivitiTaskId(), assignee, comment);
        }

        // 3. 更新 mes_approval_task 状态
        mesTask.setStatus("COMPLETED");
        mesTask.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(mesTask);

        // 4. 检查流程是否结束
        TaskService taskService = processEngine.getTaskService();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        String procInstId = process.getProcInstId();
        ProcessInstance pi = null;
        if (procInstId != null) {
            pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(procInstId)
                    .singleResult();
        }

        if (pi == null) {
            // Activiti 流程已结束（所有节点审批完成）→ 审批通过
            process.setStatus(ApprovalStatusEnum.APPROVED.getCode());
            process.setEndTime(LocalDateTime.now());
            processMapper.updateById(process);

            eventPublisher.publishEvent(
                    new ApprovalFinishedEvent(process.getBizType(), process.getBizId(), true));
            log.info("审批流程完成, processId={}, 审批通过", process.getId());
        } else {
            // 流程仍在进行 → 同步下一步任务
            List<ApprovalNode> nodes = nodeMapper.selectByTemplateId(mesTask.getTemplateId());
            ApprovalNode nextNode = findNextNode(nodes, currentNode.getNodeSeq());

            if (nextNode != null) {
                process.setCurrentNodeId(nextNode.getId());
                processMapper.updateById(process);

                // 同步 Activiti 新任务
                syncTasks(process, procInstId);
            }
        }
    }

    // ==================== 审批驳回 ====================

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long mesTaskId, String assignee, String comment) {
        ApprovalTask mesTask = taskMapper.selectById(mesTaskId);
        if (mesTask == null || !"PENDING".equals(mesTask.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        ApprovalProcess process = processMapper.selectById(mesTask.getProcessId());
        ApprovalNode node = nodeMapper.selectById(mesTask.getNodeId());

        // 1. 记录驳回动作
        recordAction(process.getId(), node.getId(), node.getNodeName(), assignee, "REJECT", comment);

        // 2. 终止 Activiti 流程实例
        if (process.getProcInstId() != null) {
            RuntimeService runtimeService = processEngine.getRuntimeService();
            try {
                runtimeService.deleteProcessInstance(process.getProcInstId(), "审批驳回: " + assignee);
            } catch (Exception e) {
                log.warn("终止 Activiti 流程实例失败, procInstId={}, msg={}", process.getProcInstId(), e.getMessage());
            }
        }

        // 3. 更新任务状态
        mesTask.setStatus("COMPLETED");
        mesTask.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(mesTask);

        // 4. 结束审批流程
        process.setStatus(ApprovalStatusEnum.REJECTED.getCode());
        process.setEndTime(LocalDateTime.now());
        processMapper.updateById(process);

        // 5. 通知业务方
        eventPublisher.publishEvent(
                new ApprovalFinishedEvent(process.getBizType(), process.getBizId(), false));

        log.info("审批被驳回, processId={}", process.getId());
    }

    // ==================== 转办 ====================

    @Transactional(rollbackFor = Exception.class)
    public void delegate(Long mesTaskId, String fromUser, String toUser, String comment) {
        ApprovalTask mesTask = taskMapper.selectById(mesTaskId);
        if (mesTask == null || !"PENDING".equals(mesTask.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        ApprovalProcess process = processMapper.selectById(mesTask.getProcessId());
        ApprovalNode node = nodeMapper.selectById(mesTask.getNodeId());

        // 记录转办
        recordAction(process.getId(), node.getId(), node.getNodeName(),
                fromUser, "DELEGATE", "转办给 " + toUser + (comment != null ? ": " + comment : ""));

        // Activiti 转办
        if (mesTask.getActivitiTaskId() != null) {
            TaskService taskService = processEngine.getTaskService();
            taskService.delegateTask(mesTask.getActivitiTaskId(), toUser);
        }

        // 更新本地待办
        mesTask.setAssignee(toUser);
        mesTask.setStatus("PENDING");
        mesTask.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(mesTask);

        log.info("审批任务已转办, taskId={}, {} → {}", mesTaskId, fromUser, toUser);
    }

    // ==================== 加签 ====================

    @Transactional(rollbackFor = Exception.class)
    public void addSign(Long mesTaskId, String operator, String addUser, String comment) {
        ApprovalTask mesTask = taskMapper.selectById(mesTaskId);
        if (mesTask == null || !"PENDING".equals(mesTask.getStatus())) {
            throw new BusinessException("审批任务不存在或已处理");
        }

        ApprovalProcess process = processMapper.selectById(mesTask.getProcessId());
        ApprovalNode node = nodeMapper.selectById(mesTask.getNodeId());

        recordAction(process.getId(), node.getId(), node.getNodeName(),
                operator, "ADD_SIGN", "加签给 " + addUser + (comment != null ? ": " + comment : ""));

        // Activiti 加签：添加候选人
        if (mesTask.getActivitiTaskId() != null) {
            TaskService taskService = processEngine.getTaskService();
            taskService.addCandidateUser(mesTask.getActivitiTaskId(), addUser);
        }

        // 新增本地待办（会签模式：需要多方审批）
        ApprovalTask newTask = new ApprovalTask();
        newTask.setProcessId(process.getId());
        newTask.setTemplateId(mesTask.getTemplateId());
        newTask.setNodeId(node.getId());
        newTask.setBizType(mesTask.getBizType());
        newTask.setBizId(mesTask.getBizId());
        newTask.setAssignee(addUser);
        newTask.setStatus("PENDING");
        newTask.setActivitiTaskId(mesTask.getActivitiTaskId());
        taskMapper.insert(newTask);

        log.info("审批任务已加签, taskId={}, 新增审批人={}", mesTaskId, addUser);
    }

    // ==================== 私有方法 ====================

    /**
     * 匹配审批模板：按优先级选第一个启用模板，is_default 兜底
     */
    private ApprovalTemplate selectTemplate(String bizType) {
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

    /**
     * 查找下一顺序节点
     */
    private ApprovalNode findNextNode(List<ApprovalNode> nodes, int currentSeq) {
        return nodes.stream()
                .filter(n -> n.getNodeSeq() > currentSeq)
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析审批人标识符
     */
    private String resolveAssignee(ApprovalNode node) {
        return switch (node.getAssigneeType()) {
            case "SPECIFIC" -> String.valueOf(node.getAssigneeId());
            case "ROLE" -> "role:" + node.getAssigneeId();
            case "DYNAMIC" -> "dynamic:" + node.getAssigneeExpr();
            default -> "sysadmin";
        };
    }

    /**
     * 完成 Activiti 任务
     */
    private void completeActivitiTask(String activitiTaskId, String assignee, String comment) {
        TaskService taskService = processEngine.getTaskService();
        Map<String, Object> vars = new HashMap<>();
        vars.put("comment", comment != null ? comment : "");
        taskService.complete(activitiTaskId, vars);
    }

    /**
     * 同步 Activiti 当前任务到 mes_approval_task 表。
     * 查找流程实例当前的 UserTask，写入本地待办。
     */
    private void syncTasks(ApprovalProcess process, String procInstId) {
        if (procInstId == null) return;

        TaskService taskService = processEngine.getTaskService();
        List<Task> activitiTasks = taskService.createTaskQuery()
                .processInstanceId(procInstId)
                .active()
                .list();

        if (activitiTasks.isEmpty()) return;

        List<ApprovalNode> templateNodes = nodeMapper.selectByTemplateId(process.getTemplateId());

        for (Task activitiTask : activitiTasks) {
            // 通过 task 名称匹配审批节点（task name = node_name）
            ApprovalNode matchedNode = templateNodes.stream()
                    .filter(n -> n.getNodeName().equals(activitiTask.getName()))
                    .findFirst()
                    .orElse(null);

            if (matchedNode == null) continue;

            // 查重：同一节点是否已有 PENDING 任务
            long existCount = taskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                    .eq(ApprovalTask::getProcessId, process.getId())
                    .eq(ApprovalTask::getNodeId, matchedNode.getId())
                    .eq(ApprovalTask::getStatus, "PENDING"));
            if (existCount > 0) continue;

            ApprovalTask mesTask = new ApprovalTask();
            mesTask.setProcessId(process.getId());
            mesTask.setTemplateId(process.getTemplateId());
            mesTask.setNodeId(matchedNode.getId());
            mesTask.setBizType(process.getBizType());
            mesTask.setBizId(process.getBizId());
            mesTask.setAssignee(activitiTask.getAssignee() != null ? activitiTask.getAssignee() : "unknown");
            mesTask.setStatus("PENDING");
            mesTask.setActivitiTaskId(activitiTask.getId());
            taskMapper.insert(mesTask);
        }
    }

    /**
     * 记录审批动作
     */
    private void recordAction(Long processId, Long nodeId, String nodeName,
                              String assignee, String action, String comment) {
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
