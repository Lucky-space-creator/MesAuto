package com.lucky.mescore.modules.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.common.serial.SerialNumberService;
import com.lucky.mescore.modules.approval.service.ApprovalEngineService;
import com.lucky.mescore.modules.purchase.entity.PurchaseRequisition;
import com.lucky.mescore.modules.purchase.mapper.PurchaseRequisitionMapper;
import com.lucky.mescore.modules.purchase.service.PurchaseRequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class PurchaseRequisitionServiceImpl extends ServiceImpl<PurchaseRequisitionMapper, PurchaseRequisition>
        implements PurchaseRequisitionService {

    private static final Set<String> SUBMITTABLE = Set.of("DRAFT", "REJECTED");

    private final SerialNumberService serialNumberService;
    private final ApprovalEngineService approvalEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRequisition(PurchaseRequisition req) {
        if (req.getMaterialId() == null) {
            throw new BusinessException("请选择采购物料");
        }
        if (!StringUtils.hasText(req.getReqNo())) {
            req.setReqNo(serialNumberService.generate("PURCHASE", "PR"));
        }
        req.setReqStatus("DRAFT");
        save(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long id, String applicant) {
        PurchaseRequisition req = getById(id);
        if (req == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (!SUBMITTABLE.contains(req.getReqStatus())) {
            throw new BusinessException("仅草稿或驳回状态的申请可提交审批");
        }
        approvalEngine.submit("PURCHASE", req.getId(), req.getReqNo(), applicant);
        req.setReqStatus("APPROVING");
        updateById(req);
    }

    public LambdaQueryWrapper<PurchaseRequisition> buildQuery(PurchaseRequisition condition) {
        LambdaQueryWrapper<PurchaseRequisition> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getReqNo() != null, PurchaseRequisition::getReqNo, condition.getReqNo())
              .like(condition.getTitle() != null, PurchaseRequisition::getTitle, condition.getTitle())
              .eq(condition.getReqStatus() != null, PurchaseRequisition::getReqStatus, condition.getReqStatus());
        }
        qw.orderByDesc(PurchaseRequisition::getCreateTime);
        return qw;
    }
}
