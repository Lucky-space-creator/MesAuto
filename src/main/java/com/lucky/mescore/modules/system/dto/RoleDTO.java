package com.lucky.mescore.modules.system.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class RoleDTO {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;
    private Integer status;
    private java.util.List<Long> permissionIds;
}
