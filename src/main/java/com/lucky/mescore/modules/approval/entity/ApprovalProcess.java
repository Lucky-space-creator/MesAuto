package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mes_approval_process")
public class ApprovalProcess {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long templateId;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Long currentNodeId;
    private String status;
    private String applicant;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
