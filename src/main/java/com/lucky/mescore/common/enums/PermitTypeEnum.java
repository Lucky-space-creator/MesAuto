package com.lucky.mescore.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermitTypeEnum {

    MENU("MENU", "菜单"),
    BTN("BTN", "按钮"),
    API("API", "接口");

    private final String code;
    private final String desc;
}
