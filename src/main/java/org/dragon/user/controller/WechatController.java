package org.dragon.user.controller;

import org.dragon.user.dto.ApiResponse;
import org.dragon.user.dto.LoginResponse;
import org.dragon.user.service.WechatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WechatController 微信登录接口
 */
@RestController
@RequestMapping("/api/user")
public class WechatController {

    private final WechatService wechatService;

    public WechatController(WechatService wechatService) {
        this.wechatService = wechatService;
    }

    /**
     * 获取微信登录二维码
     */
    @PostMapping("/wechat/qrcode")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getQrCode() {
        Map<String, Object> result = wechatService.getQrCode();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 微信登录回调
     */
    @PostMapping("/wechat/callback")
    public ResponseEntity<ApiResponse<LoginResponse>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state) {
        LoginResponse response = wechatService.callback(code, state);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
