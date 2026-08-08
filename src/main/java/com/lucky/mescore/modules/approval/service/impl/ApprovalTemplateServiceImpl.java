package com.lucky.mescore.modules.approval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.approval.entity.ApprovalNode;
import com.lucky.mescore.modules.approval.entity.ApprovalTemplate;
import com.lucky.mescore.modules.approval.mapper.ApprovalNodeMapper;
import com.lucky.mescore.modules.approval.mapper.ApprovalTemplateMapper;
import com.lucky.mescore.modules.approval.service.ApprovalTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalTemplateServiceImpl extends ServiceImpl<ApprovalTemplateMapper, ApprovalTemplate> implements ApprovalTemplateService {

    private final ApprovalNodeMapper nodeMapper;

    @Override
    public List<ApprovalNode> getNodes(Long templateId) {
        return nodeMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTemplateWithNodes(ApprovalTemplate template, List<ApprovalNode> nodes) {
        save(template);
        if (nodes != null && !nodes.isEmpty()) {
            nodes.forEach(node -> {
                node.setTemplateId(template.getId());
                nodeMapper.insert(node);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateWithNodes(ApprovalTemplate template, List<ApprovalNode> nodes) {
        updateById(template);
        if (nodes != null) {
            nodeMapper.deleteByTemplateId(template.getId());
            nodes.forEach(node -> {
                node.setId(null);
                node.setTemplateId(template.getId());
                nodeMapper.insert(node);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long templateId) {
        ApprovalTemplate template = getById(templateId);
        if (template == null) {
            throw new BusinessException("审批模板不存在");
        }
        template.setStatus("PUBLISHED");
        updateById(template);
    }
}
