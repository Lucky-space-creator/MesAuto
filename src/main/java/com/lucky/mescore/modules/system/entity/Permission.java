package com.lucky.mescore.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    private Long parentId;
    private String permCode;
    private String permName;
    private String permType;
    private String url;
    private String icon;
    private Integer sort;
    private Integer status;
}
