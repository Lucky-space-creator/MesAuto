package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
    /** 审批节点列表，非数据库字段，仅在创建/更新时一并提交 */
    @TableField(exist = false)
    private List<ApprovalNode> nodes;
}
