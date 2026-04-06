package org.dragon.user.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtService JWT工具类
 */
@Component
public class JwtService {

    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("jwt.secret")
    @Value("${jwt.secret}")
    private String jwtSecret;

    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("jwt.access-token-validity")
    @Value("${jwt.access-token-validity:7200}")
    private long accessTokenValidity; // 秒

    // TODO [ConfigStore Migration]: 迁移到 ConfigStore GLOBAL scope，使用 ConfigKey.of("jwt.refresh-token-validity")
    @Value("${jwt.refresh-token-validity:604800}")
    private long refreshTokenValidity; // 秒

    /**
     * 生成AccessToken
     */
    public String generateAccessToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "access");
        return createToken(claims, userId, accessTokenValidity * 1000);
    }

    /**
     * 生成RefreshToken
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        return createToken(claims, userId, refreshTokenValidity * 1000);
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取Token类型
     */
    public String getTypeFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * 检查是否是AccessToken
     */
    public boolean isAccessToken(String token) {
        return "access".equals(getTypeFromToken(token));
    }

    /**
     * 检查是否是RefreshToken
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(getTypeFromToken(token));
    }

    /**
     * 获取AccessToken过期时间（秒）
     */
    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    /**
     * 获取RefreshToken过期时间（秒）
     */
    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    private String createToken(Map<String, Object> claims, Long userId, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
