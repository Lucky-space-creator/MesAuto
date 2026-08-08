package com.lucky.mescore.modules.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.purchase.entity.PurchaseRequisition;

public interface PurchaseRequisitionService extends IService<PurchaseRequisition> {

    /** 新增采购申请（草稿态） */
    void createRequisition(PurchaseRequisition req);

    /** 提交审批：触发审批引擎并将状态置为 APPROVING */
    void submitApproval(Long id, String applicant);

    /** 构建查询条件 */
    LambdaQueryWrapper<PurchaseRequisition> buildQuery(PurchaseRequisition condition);
}
