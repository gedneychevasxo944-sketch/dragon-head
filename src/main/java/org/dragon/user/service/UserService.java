package org.dragon.user.service;

import org.dragon.store.StoreFactory;
import org.dragon.user.dto.*;
import org.dragon.datasource.entity.UserEntity;
import org.dragon.datasource.entity.UserTokenEntity;
import org.dragon.user.security.service.JwtService;
import org.dragon.user.store.TokenStore;
import org.dragon.user.store.UserStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserService 用户服务
 */
@Service
public class UserService {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserStore userStore;
    private final TokenStore tokenStore;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserService(StoreFactory storeFactory, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userStore = storeFactory.get(UserStore.class);
        this.tokenStore = storeFactory.get(TokenStore.class);
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户注册
     */
    @Transactional
    public UserInfo register(RegisterRequest request) {
        // 检查用户名是否存在
        if (userStore.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 创建用户
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .status("NORMAL")
                .loginFailCount(0)
                .build();

        userStore.save(user);

        return toUserInfo(user);
    }

    /**
     * 密码登录
     */
    @Transactional
    public LoginResponse passwordLogin(LoginRequest request, String ip) {
        UserEntity user = userStore.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // 检查账户是否被锁定
        if (user.isLocked()) {
            throw new IllegalArgumentException("账户已锁定，请在" + LOCK_MINUTES + "分钟后重试");
        }

        // 检查账户是否可用
        if (!user.isActive()) {
            throw new IllegalArgumentException("账户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 增加失败计数
            user.setLoginFailCount(user.getLoginFailCount() + 1);
            if (user.getLoginFailCount() >= MAX_LOGIN_FAIL_COUNT) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            userStore.update(user);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 登录成功，重置失败计数
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userStore.update(user);

        return createLoginResponse(user);
    }

    /**
     * 刷新Token
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        // 验证refreshToken
        if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("无效的refreshToken");
        }

        // 查找Token记录
        UserTokenEntity tokenEntity = tokenStore.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Token不存在或已过期"));

        // 检查是否过期
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenStore.deleteById(tokenEntity.getId());
            throw new IllegalArgumentException("Token已过期");
        }

        // 获取用户
        UserEntity user = userStore.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 删除旧的refreshToken
        tokenStore.deleteById(tokenEntity.getId());

        // 创建新的登录响应
        return createLoginResponse(user);
    }

    /**
     * 登出
     */
    @Transactional
    public void logout(Long userId) {
        tokenStore.deleteByUserId(userId);
    }

    /**
     * 获取当前用户信息
     */
    public UserInfo getCurrentUser(Long userId) {
        UserEntity user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toUserInfo(user);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public UserInfo updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        userStore.update(user);
        return toUserInfo(user);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, PasswordRequest request) {
        UserEntity user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userStore.update(user);

        // 使所有refreshToken失效
        tokenStore.deleteByUserId(userId);
    }

    private LoginResponse createLoginResponse(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 保存refreshToken
        UserTokenEntity tokenEntity = UserTokenEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenValidity()))
                .build();
        tokenStore.save(tokenEntity);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenValidity())
                .user(toUserInfo(user))
                .build();
    }

    private UserInfo toUserInfo(UserEntity user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }
}
