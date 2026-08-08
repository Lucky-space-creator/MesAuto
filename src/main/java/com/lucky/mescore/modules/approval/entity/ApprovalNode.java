package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mes_approval_node")
public class ApprovalNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private Integer nodeSeq;
    private String nodeName;
    private String assigneeType;
    private Long assigneeId;
    private String assigneeExpr;
    private String conditionExpr;
    private Integer timeoutHours;
    private Integer allowDelegate;
    private Integer allowAddSign;
    private String signType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
