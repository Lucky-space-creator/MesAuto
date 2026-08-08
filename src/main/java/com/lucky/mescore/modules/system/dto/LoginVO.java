package com.lucky.mescore.modules.system.dto;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
}
