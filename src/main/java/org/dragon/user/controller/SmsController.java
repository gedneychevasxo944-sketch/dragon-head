package org.dragon.user.controller;

import jakarta.validation.Valid;
import org.dragon.user.dto.*;
import org.dragon.user.security.filter.JwtAuthenticationFilter.UserPrincipal;
import org.dragon.user.service.SmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * SmsController 短信验证码接口
 */
@RestController
@RequestMapping("/api/user")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/sms/send")
    public ResponseEntity<ApiResponse<Void>> sendCode(@Valid @RequestBody SmsSendRequest request) {
        smsService.sendCode(request);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    /**
     * 验证码登录
     */
    @PostMapping("/login/sms/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyCode(@Valid @RequestBody SmsVerifyRequest request) {
        LoginResponse response = smsService.verifyCode(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 发送绑定手机验证码（需要登录）
     */
    @PostMapping("/phone/send-code")
    public ResponseEntity<ApiResponse<Void>> sendBindCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SmsSendRequest request) {
        smsService.sendBindCode(principal.userId(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    /**
     * 绑定手机号（需要登录）
     */
    @PostMapping("/phone/bind")
    public ResponseEntity<ApiResponse<Void>> bindPhone(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SmsVerifyRequest request) {
        smsService.bindPhone(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("手机号绑定成功", null));
    }
}
