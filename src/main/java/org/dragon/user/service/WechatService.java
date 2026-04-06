package org.dragon.user.service;

import org.dragon.store.StoreFactory;
import org.dragon.user.dto.LoginResponse;
import org.dragon.user.dto.UserInfo;
import org.dragon.datasource.entity.UserEntity;
import org.dragon.datasource.entity.UserTokenEntity;
import org.dragon.user.security.service.JwtService;
import org.dragon.user.store.TokenStore;
import org.dragon.user.store.UserStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WechatService 微信登录服务（占位实现）
 */
@Service
public class WechatService {

    private final UserStore userStore;
    private final TokenStore tokenStore;
    private final JwtService jwtService;

    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("wechat.app-id")
    @Value("${wechat.app-id:}")
    private String appId;

    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("wechat.app-secret")
    @Value("${wechat.app-secret:}")
    private String appSecret;

    public WechatService(StoreFactory storeFactory, JwtService jwtService) {
        this.userStore = storeFactory.get(UserStore.class);
        this.tokenStore = storeFactory.get(TokenStore.class);
        this.jwtService = jwtService;
    }

    /**
     * 获取微信登录二维码
     */
    public Map<String, Object> getQrCode() {
        if (appId.isEmpty() || appSecret.isEmpty()) {
            throw new IllegalStateException("微信登录未配置");
        }

        // TODO: 调用微信开放平台API获取授权码
        // 实际实现需要：
        // 1. 调用微信开放平台 https://open.weixin.qq.com/connect/qrconnect 获取授权码
        // 2. 返回二维码地址或前端自行生成二维码

        Map<String, Object> result = new HashMap<>();
        result.put("qrcodeUrl", "https://open.weixin.qq.com/connect/qrconnect?appid=" + appId + "&response_type=code&scope=snsapi_login&redirect_uri=");
        result.put("state", UUID.randomUUID().toString()); // 用于防止CSRF
        result.put("message", "微信登录配置占位，请配置 wechat.app-id 和 wechat.app-secret");

        return result;
    }

    /**
     * 微信登录回调
     */
    @Transactional
    public LoginResponse callback(String code, String state) {
        if (appId.isEmpty() || appSecret.isEmpty()) {
            throw new IllegalStateException("微信登录未配置");
        }

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("授权码不能为空");
        }

        // TODO: 调用微信开放平台API用code换取access_token和openid
        // 实际实现需要：
        // 1. 用code换取access_token: https://api.weixin.qq.com/sns/oauth2/access_token
        // 2. 用access_token获取用户信息: https://api.weixin.qq.com/sns/userinfo
        // 3. 根据openid查找或创建用户

        // 占位实现：模拟登录
        throw new IllegalStateException("微信登录功能占位，请先配置 wechat.app-id 和 wechat.app-secret");
    }

    /**
     * 微信扫码登录（前端直接获取openid的场景）
     */
    @Transactional
    public LoginResponse loginByOpenid(String openid, String nickname, String avatar) {
        // 查找是否已有用户绑定此openid
        // TODO: 需要在用户表中添加wechat_openid字段
        // 目前占位实现

        throw new IllegalStateException("微信登录功能占位，需要在adeptify_user表中添加wechat_openid字段");
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
