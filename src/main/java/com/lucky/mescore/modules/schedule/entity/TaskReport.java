package com.lucky.mescore.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_task_report")
public class TaskReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String reportType;
    private BigDecimal reportQty;
    private BigDecimal defectiveQty;
    private BigDecimal workHours;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
