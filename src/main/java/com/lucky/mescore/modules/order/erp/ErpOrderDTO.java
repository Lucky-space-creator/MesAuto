package com.lucky.mescore.modules.order.erp;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 上游 ERP 下发的销售/生产订单报文结构。
 *
 * 这是 MES 与 ERP 之间的契约对象，字段命名保持 ERP 侧口径，
 * 由 {@link ErpOrderSyncService} 负责翻译成 MES 内部的 Order。
 */
@Data
public class ErpOrderDTO {

    /** ERP 侧单号，MES 用它做幂等去重 */
    private String erpOrderNo;

    /** 产品编码，MES 按 material_code 匹配物料主数据 */
    private String materialCode;

    /** 数量 */
    private BigDecimal quantity;

    /** 单位编码 */
    private String unitCode;

    /** 客户名称 */
    private String customerName;

    /** 需求开始日期 */
    private LocalDate planStartDate;

    /** 交期 */
    private LocalDate planEndDate;

    /** 优先级：NORMAL / URGENT */
    private String priority;

    private String remark;
}
