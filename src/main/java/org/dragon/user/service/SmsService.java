package org.dragon.user.service;

import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import org.dragon.store.StoreFactory;
import org.dragon.user.dto.LoginResponse;
import org.dragon.user.dto.SmsSendRequest;
import org.dragon.user.dto.SmsVerifyRequest;
import org.dragon.user.dto.UserInfo;
import org.dragon.datasource.entity.SmsCodeEntity;
import org.dragon.datasource.entity.UserEntity;
import org.dragon.datasource.entity.UserTokenEntity;
import org.dragon.user.security.service.JwtService;
import org.dragon.user.store.SmsCodeStore;
import org.dragon.user.store.TokenStore;
import org.dragon.user.store.UserStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * SmsService 短信验证码服务
 */
@Service
public class SmsService {

    private static final int DEFAULT_CODE_VALIDITY_MINUTES = 5;
    private static final int DEFAULT_CODE_SEND_COOLDOWN_SECONDS = 60;
    /** @deprecated use DEFAULT_CODE_VALIDITY_MINUTES */
    @Deprecated
    public static final int CODE_VALIDITY_MINUTES = DEFAULT_CODE_VALIDITY_MINUTES;
    /** @deprecated use DEFAULT_CODE_SEND_COOLDOWN_SECONDS */
    @Deprecated
    public static final int CODE_SEND_COOLDOWN_SECONDS = DEFAULT_CODE_SEND_COOLDOWN_SECONDS;

    private final SmsCodeStore smsCodeStore;
    private final UserStore userStore;
    private final TokenStore tokenStore;
    private final JwtService jwtService;

    private String aliyunAccessKey;
    private String aliyunAccessSecret;

    public SmsService(StoreFactory storeFactory, JwtService jwtService, ConfigApplication configApplication) {
        this.smsCodeStore = storeFactory.get(SmsCodeStore.class);
        this.userStore = storeFactory.get(UserStore.class);
        this.tokenStore = storeFactory.get(TokenStore.class);
        this.jwtService = jwtService;
        InheritanceContext ctx = InheritanceContext.forGlobal();
        this.aliyunAccessKey = configApplication.getStringValue("sms.aliyun.access-key", ctx, "");
        this.aliyunAccessSecret = configApplication.getStringValue("sms.aliyun.access-secret", ctx, "");
    }

    /**
     * 发送验证码
     */
    @Transactional
    public void sendCode(SmsSendRequest request) {
        // 检查是否配置了阿里云短信
        if (aliyunAccessKey.isEmpty() || aliyunAccessSecret.isEmpty()) {
            throw new IllegalStateException("短信服务未配置");
        }

        // 检查发送冷却
        smsCodeStore.findLatest(request.getPhone(), request.getType())
                .ifPresent(code -> {
                    if (code.getCreateTime().plusSeconds(CODE_SEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                        throw new IllegalArgumentException("发送太频繁，请" + CODE_SEND_COOLDOWN_SECONDS + "秒后重试");
                    }
                });

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(999999));

        // TODO: 实际调用阿里云短信API发送验证码
        // 目前仅保存到数据库用于测试
        SmsCodeEntity smsCode = SmsCodeEntity.builder()
                .id(UUID.randomUUID().toString())
                .phone(request.getPhone())
                .code(code)
                .type(request.getType())
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES))
                .used(false)
                .build();

        smsCodeStore.save(smsCode);

        // TODO: 实际发送短信，这里只是记录日志
        System.out.println("【SMS】发送给 " + request.getPhone() + " 的验证码: " + code);
    }

    /**
     * 验证码登录
     */
    @Transactional
    public LoginResponse verifyCode(SmsVerifyRequest request) {
        // 查找最新的验证码
        SmsCodeEntity smsCode = smsCodeStore.findLatest(request.getPhone(), "LOGIN")
                .orElseThrow(() -> new IllegalArgumentException("验证码不存在或已过期"));

        // 验证验证码
        if (!smsCode.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 标记已使用
        smsCodeStore.markUsed(smsCode.getId());

        // 查找或创建用户（手机号已注册则直接登录，否则创建新用户）
        UserEntity user = userStore.findByPhone(request.getPhone())
                .orElseGet(() -> createUserByPhone(request.getPhone()));

        return createLoginResponse(user);
    }

    /**
     * 绑定手机号
     */
    @Transactional
    public void bindPhone(Long userId, SmsVerifyRequest request) {
        // 查找验证码
        SmsCodeEntity smsCode = smsCodeStore.findLatest(request.getPhone(), "BIND_PHONE")
                .orElseThrow(() -> new IllegalArgumentException("验证码不存在或已过期"));

        // 验证验证码
        if (!smsCode.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("验证码错误");
        }

        // 标记已使用
        smsCodeStore.markUsed(smsCode.getId());

        // 检查手机号是否已被其他用户绑定
        userStore.findByPhone(request.getPhone())
                .ifPresent(u -> {
                    if (!u.getId().equals(userId)) {
                        throw new IllegalArgumentException("手机号已被其他用户绑定");
                    }
                });

        // 更新用户手机号
        userStore.findById(userId).ifPresent(user -> {
            user.setPhone(request.getPhone());
            userStore.update(user);
        });
    }

    /**
     * 发送绑定手机验证码
     */
    @Transactional
    public void sendBindCode(Long userId, String phone) {
        // 检查手机号是否已被绑定
        userStore.findByPhone(phone).ifPresent(u -> {
            if (!u.getId().equals(userId)) {
                throw new IllegalArgumentException("手机号已被其他用户绑定");
            }
        });

        SmsSendRequest request = new SmsSendRequest();
        request.setPhone(phone);
        request.setType("BIND_PHONE");
        sendCode(request);
    }

    private UserEntity createUserByPhone(String phone) {
        UserEntity user = UserEntity.builder()
                .username("user_" + phone.substring(phone.length() - 4))
                .phone(phone)
                .nickname("用户" + phone.substring(phone.length() - 4))
                .passwordHash("") // 短信登录用户无密码
                .status("NORMAL")
                .loginFailCount(0)
                .build();
        userStore.save(user);
        return user;
    }

    private LoginResponse createLoginResponse(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

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
                .user(UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }
}
