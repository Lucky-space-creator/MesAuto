package com.lucky.mescore.modules.approval.engine;

import com.lucky.mescore.modules.approval.entity.ApprovalNode;
import com.lucky.mescore.modules.approval.entity.ApprovalTemplate;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BPMN 2.0 XML 生成器：将审批模板 + 节点动态渲染为 Activiti 可部署的流程定义。
 *
 * <p>渲染规则：
 * <ul>
 *   <li>顺序节点 → userTask + sequenceFlow 链</li>
 *   <li>相同 nodeSeq + 不同 ID 的并行节点 → parallelGateway fork/join + 多 userTask</li>
 *   <li>ROLE 审批人 → candidateGroups 变量</li>
 *   <li>SPECIFIC 审批人 → assignee 变量</li>
 *   <li>DYNAMIC 审批人 → assignee 表达式变量</li>
 * </ul>
 */
public class BpmnXmlGenerator {

    private static final String BPMN_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String ACTIVITI_NS = "http://activiti.org/bpmn";

    private BpmnXmlGenerator() {}

    /**
     * 生成流程定义 XML。
     *
     * @param template 审批模板
     * @param nodes    模板下所有审批节点（已按 node_seq 排序）
     * @return BPMN 2.0 XML 字符串，可直接部署到 Activiti
     */
    public static String generate(ApprovalTemplate template, List<ApprovalNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("审批节点不能为空");
        }

        try {
            StringWriter sw = new StringWriter();
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter w = factory.createXMLStreamWriter(sw);

            w.writeStartDocument("UTF-8", "1.0");
            w.writeStartElement("definitions");
            w.writeAttribute("xmlns", BPMN_NS);
            w.writeAttribute("xmlns:activiti", ACTIVITI_NS);
            w.writeAttribute("targetNamespace", "http://www.activiti.org/mes");
            w.writeAttribute("id", "def_" + template.getTemplateCode());

            String processId = "process_" + template.getTemplateCode();
            w.writeStartElement("process");
            w.writeAttribute("id", processId);
            w.writeAttribute("name", template.getTemplateName());
            w.writeAttribute("isExecutable", "true");

            // 按 nodeSeq 分组：同一 nodeSeq 出现多次 = 并行节点
            Map<Integer, List<ApprovalNode>> grouped = nodes.stream()
                    .collect(Collectors.groupingBy(ApprovalNode::getNodeSeq, LinkedHashMap::new, Collectors.toList()));

            List<Map.Entry<Integer, List<ApprovalNode>>> entries = new ArrayList<>(grouped.entrySet());

            // === Start Event ===
            String startId = "startEvent";
            writeStartEvent(w, startId);

            // 前置网关（如果有并行节点则直接 fork）
            String firstTargetId;
            List<ApprovalNode> firstGroup = entries.get(0).getValue();
            if (firstGroup.size() > 1) {
                String forkId = "fork_0";
                writeParallelGateway(w, forkId);
                w.writeEmptyElement("sequenceFlow");
                w.writeAttribute("id", "flow_start_" + forkId);
                w.writeAttribute("sourceRef", startId);
                w.writeAttribute("targetRef", forkId);
                for (int i = 0; i < firstGroup.size(); i++) {
                    ApprovalNode node = firstGroup.get(i);
                    String taskId = nodeTaskId(node);
                    writeUserTask(w, taskId, node);
                    w.writeEmptyElement("sequenceFlow");
                    w.writeAttribute("id", "flow_" + forkId + "_" + taskId);
                    w.writeAttribute("sourceRef", forkId);
                    w.writeAttribute("targetRef", taskId);
                }
                firstTargetId = null; // fork directly from start
            } else {
                ApprovalNode firstNode = firstGroup.get(0);
                String taskId = nodeTaskId(firstNode);
                writeUserTask(w, taskId, firstNode);
                w.writeEmptyElement("sequenceFlow");
                w.writeAttribute("id", "flow_start_" + taskId);
                w.writeAttribute("sourceRef", startId);
                w.writeAttribute("targetRef", taskId);
                firstTargetId = taskId;
            }

            // === 中间节点 ===
            String prevTargetId = firstTargetId;
            boolean prevWasParallel = (firstGroup.size() > 1);
            String prevJoinId = null;

            for (int gi = 1; gi < entries.size(); gi++) {
                List<ApprovalNode> group = entries.get(gi).getValue();
                boolean isParallel = (group.size() > 1);

                if (isParallel) {
                    // 前一节点 → fork gateway
                    String forkId = "fork_" + gi;
                    writeParallelGateway(w, forkId);

                    if (prevWasParallel) {
                        // 从上一组的 join 连到本组 fork
                        String srcFlowId = prevJoinId;
                        w.writeEmptyElement("sequenceFlow");
                        w.writeAttribute("id", "flow_" + srcFlowId + "_" + forkId);
                        w.writeAttribute("sourceRef", srcFlowId);
                        w.writeAttribute("targetRef", forkId);
                    } else {
                        w.writeEmptyElement("sequenceFlow");
                        w.writeAttribute("id", "flow_" + prevTargetId + "_" + forkId);
                        w.writeAttribute("sourceRef", prevTargetId);
                        w.writeAttribute("targetRef", forkId);
                    }

                    // fork → 各 task
                    for (ApprovalNode node : group) {
                        String taskId = nodeTaskId(node);
                        writeUserTask(w, taskId, node);
                        w.writeEmptyElement("sequenceFlow");
                        w.writeAttribute("id", "flow_" + forkId + "_" + taskId);
                        w.writeAttribute("sourceRef", forkId);
                        w.writeAttribute("targetRef", taskId);
                    }
                    prevWasParallel = true;
                    prevTargetId = null;
                } else {
                    // 顺序节点
                    ApprovalNode node = group.get(0);
                    String taskId = nodeTaskId(node);
                    writeUserTask(w, taskId, node);

                    if (prevWasParallel) {
                        // 上一个并行组的 join 网关连到当前节点
                        String joinId = "join_" + (gi - 1);
                        writeParallelGateway(w, joinId);
                        prevJoinId = joinId;
                        // 各并行 task → join
                        List<ApprovalNode> prevGroup = entries.get(gi - 1).getValue();
                        for (ApprovalNode pn : prevGroup) {
                            w.writeEmptyElement("sequenceFlow");
                            w.writeAttribute("id", "flow_" + nodeTaskId(pn) + "_" + joinId);
                            w.writeAttribute("sourceRef", nodeTaskId(pn));
                            w.writeAttribute("targetRef", joinId);
                        }
                        // join → 当前 task
                        w.writeEmptyElement("sequenceFlow");
                        w.writeAttribute("id", "flow_" + joinId + "_" + taskId);
                        w.writeAttribute("sourceRef", joinId);
                        w.writeAttribute("targetRef", taskId);
                    } else {
                        w.writeEmptyElement("sequenceFlow");
                        w.writeAttribute("id", "flow_" + prevTargetId + "_" + taskId);
                        w.writeAttribute("sourceRef", prevTargetId);
                        w.writeAttribute("targetRef", taskId);
                    }
                    prevWasParallel = false;
                    prevTargetId = taskId;
                }
            }

            // === 最后是并行组时，需要闭合 join → end ===
            Map.Entry<Integer, List<ApprovalNode>> lastEntry = entries.get(entries.size() - 1);
            if (lastEntry.getValue().size() > 1) {
                String joinId = "join_" + (entries.size() - 1);
                writeParallelGateway(w, joinId);
                for (ApprovalNode pn : lastEntry.getValue()) {
                    w.writeEmptyElement("sequenceFlow");
                    w.writeAttribute("id", "flow_" + nodeTaskId(pn) + "_" + joinId);
                    w.writeAttribute("sourceRef", nodeTaskId(pn));
                    w.writeAttribute("targetRef", joinId);
                }
                String endId = "endEvent";
                writeEndEvent(w, endId);
                w.writeEmptyElement("sequenceFlow");
                w.writeAttribute("id", "flow_" + joinId + "_end");
                w.writeAttribute("sourceRef", joinId);
                w.writeAttribute("targetRef", endId);
            } else {
                // 直接从前一 task → end
                String endId = "endEvent";
                writeEndEvent(w, endId);
                w.writeEmptyElement("sequenceFlow");
                w.writeAttribute("id", "flow_" + prevTargetId + "_end");
                w.writeAttribute("sourceRef", prevTargetId);
                w.writeAttribute("targetRef", endId);
            }

            w.writeEndElement(); // process
            w.writeEndElement(); // definitions
            w.writeEndDocument();
            w.flush();
            w.close();

            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成 BPMN XML 失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有工具方法 ====================

    private static String nodeTaskId(ApprovalNode node) {
        return "task_" + node.getId();
    }

    private static void writeStartEvent(XMLStreamWriter w, String id) throws Exception {
        w.writeEmptyElement("startEvent");
        w.writeAttribute("id", id);
    }

    private static void writeEndEvent(XMLStreamWriter w, String id) throws Exception {
        w.writeEmptyElement("endEvent");
        w.writeAttribute("id", id);
    }

    private static void writeParallelGateway(XMLStreamWriter w, String id) throws Exception {
        w.writeEmptyElement("parallelGateway");
        w.writeAttribute("id", id);
    }

    private static void writeUserTask(XMLStreamWriter w, String id, ApprovalNode node) throws Exception {
        w.writeStartElement("userTask");
        w.writeAttribute("id", id);
        w.writeAttribute("name", node.getNodeName());
        w.writeAttribute("activiti:assignee", "${assignee_" + node.getId() + "}");
        w.writeAttribute("activiti:candidateGroups", "${candidateGroups_" + node.getId() + "}");
        w.writeEndElement();
    }
}
