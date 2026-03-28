package org.dragon.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 短信发送请求
 */
@Data
public class SmsSendRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码类型不能为空")
    private String type; // LOGIN, BIND_PHONE, FORGET_PASSWORD
}
