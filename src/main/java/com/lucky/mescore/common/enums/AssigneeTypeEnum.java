package com.lucky.mescore.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AssigneeTypeEnum {

    ROLE("ROLE", "按角色"),
    SPECIFIC("SPECIFIC", "指定人"),
    DYNAMIC("DYNAMIC", "动态表达式");

    private final String code;
    private final String desc;
}
