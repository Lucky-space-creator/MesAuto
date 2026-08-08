package com.lucky.mescore.modules.system.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;
    private String phone;
    private String email;
    private Integer status;
    private java.util.List<Long> roleIds;
}
