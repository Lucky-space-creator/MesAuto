package com.lucky.mescore.modules.system.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
    private String username;
    private String realName;
    private String phone;
    private Integer status;
}
