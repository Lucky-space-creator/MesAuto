package com.lucky.mescore.modules.process.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_route")
public class ProcessRoute extends BaseEntity {
    private String routeCode;
    private String routeName;
    private Long materialId;
    /** 关联物料名称，非数据库字段，仅用于前端展示 */
    @TableField(exist = false)
    private String materialName;
    private String version;
    private String status;
    private LocalDate effectiveDate;
    private LocalDate expiredDate;
    private String remark;
    /** 工序步骤列表，非数据库字段，仅在创建/更新时一并提交 */
    @TableField(exist = false)
    private List<ProcessStep> steps;
}
