package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mes_approval_task")
public class ApprovalTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long processId;
    private Long templateId;
    private Long nodeId;
    private String bizType;
    private Long bizId;
    private String assignee;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String nodeName;
    @TableField(exist = false)
    private String bizNo;
}
