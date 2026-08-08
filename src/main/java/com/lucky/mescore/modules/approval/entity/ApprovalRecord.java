package com.lucky.mescore.modules.approval.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mes_approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long processId;
    private Long nodeId;
    private String nodeName;
    private String assignee;
    private String action;
    private String comment;
    private LocalDateTime createTime;
}
