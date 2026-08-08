package com.lucky.mescore.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.editable.EditableWhen;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 生产订单。
 *
 * 字段可编辑性说明（见 {@link EditableWhen}）：
 * - 未标注注解的字段（orderNo/orderStatus/completedQty/实际日期/来源字段）
 *   均由系统或上游 ERP 维护，任何状态下都不允许通过更新接口修改；
 * - 主数据类字段仅草稿态可改；
 * - 计划日期在草稿与已下达态可改（现场排产常需微调）；
 * - 优先级和备注在生产过程中仍可调整。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_order")
public class Order extends BaseEntity {

    /** 订单号，由流水号生成器或 ERP 下发，不可人工修改 */
    private String orderNo;

    @EditableWhen({"DRAFT"})
    private String orderType;

    /** 订单状态，只能通过专门的状态流转接口变更 */
    private String orderStatus;

    @EditableWhen({"DRAFT"})
    private Long materialId;

    @EditableWhen({"DRAFT"})
    private BigDecimal plannedQty;

    /** 完工数量由报工汇总回写，不允许手工改 */
    private BigDecimal completedQty;

    @EditableWhen({"DRAFT"})
    private Long unitId;

    /** 优先级允许在下达和生产过程中调整，以支持插单 */
    @EditableWhen({"DRAFT", "RELEASED", "IN_PRODUCTION"})
    private String priority;

    @EditableWhen({"DRAFT", "RELEASED"})
    private LocalDate planStartDate;

    @EditableWhen({"DRAFT", "RELEASED"})
    private LocalDate planEndDate;

    /** 实际开工日期由下达动作写入 */
    private LocalDate actualStartDate;

    /** 实际完工日期由完工动作写入 */
    private LocalDate actualEndDate;

    @EditableWhen({"DRAFT"})
    private Long bomId;

    @EditableWhen({"DRAFT"})
    private Long routeId;

    @EditableWhen({"DRAFT"})
    private String customerName;

    /** 备注全程可维护 */
    @EditableWhen({"*"})
    private String remark;

    // ==================== 上游 ERP 同步相关字段 ====================

    /** 订单来源：MANUAL=手工创建，ERP=上游ERP同步 */
    private String sourceType;

    /** 上游 ERP 单号，用于幂等去重 */
    private String sourceNo;

    /** 最近一次从 ERP 同步的时间 */
    private LocalDateTime syncTime;

    // ==================== 非持久化：前端展示用关联名称 ====================

    @TableField(exist = false)
    private String materialName;

    @TableField(exist = false)
    private String bomName;

    @TableField(exist = false)
    private String routeName;
}
