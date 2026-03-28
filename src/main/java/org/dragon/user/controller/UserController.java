package org.dragon.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.dragon.user.dto.*;
import org.dragon.user.security.filter.JwtAuthenticationFilter.UserPrincipal;
import org.dragon.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * UserController 用户接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserInfo>> register(@Valid @RequestBody RegisterRequest request) {
        UserInfo user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 密码登录
     */
    @PostMapping("/login/password")
    public ResponseEntity<ApiResponse<LoginResponse>> passwordLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        LoginResponse response = userService.passwordLogin(request, ip);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = userService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        userService.logout(principal.userId());
        return ResponseEntity.ok(ApiResponse.success("登出成功", null));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserInfo user = userService.getCurrentUser(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateUserRequest request) {
        UserInfo user = userService.updateUser(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PasswordRequest request) {
        userService.changePassword(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
