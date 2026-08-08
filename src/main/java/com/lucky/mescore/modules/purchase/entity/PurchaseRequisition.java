package com.lucky.mescore.modules.purchase.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_purchase_requisition")
public class PurchaseRequisition extends BaseEntity {

    /** 采购申请单号，由流水号生成 */
    private String reqNo;

    /** 申请主题 */
    private String title;

    /** 物料ID */
    private Long materialId;

    /** 计划采购数量 */
    private BigDecimal planQty;

    /** 单位ID */
    private Long unitId;

    /** 期望到货日期 */
    private java.time.LocalDate expectDate;

    /** 申请状态：DRAFT 草稿 / APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 */
    private String reqStatus;

    private String remark;

    // ==================== 非持久化：前端展示用关联名称 ====================
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unitName;
}
