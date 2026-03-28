package org.dragon.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class PasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
