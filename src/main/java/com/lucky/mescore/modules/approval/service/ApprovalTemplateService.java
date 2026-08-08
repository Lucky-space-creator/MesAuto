package com.lucky.mescore.modules.approval.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.approval.entity.ApprovalNode;
import com.lucky.mescore.modules.approval.entity.ApprovalTemplate;

import java.util.List;

public interface ApprovalTemplateService extends IService<ApprovalTemplate> {

    List<ApprovalNode> getNodes(Long templateId);

    void saveTemplateWithNodes(ApprovalTemplate template, List<ApprovalNode> nodes);

    void updateTemplateWithNodes(ApprovalTemplate template, List<ApprovalNode> nodes);

    void publish(Long templateId);
}
