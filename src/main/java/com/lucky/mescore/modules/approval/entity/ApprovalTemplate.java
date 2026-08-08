package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_approval_template")
public class ApprovalTemplate extends BaseEntity {
    private String templateCode;
    private String templateName;
    private String bizType;
    private String bizCategory;
    private String description;
    private Integer priority;
    private Integer isDefault;
    private String conditionExpr;
    private String status;
}
