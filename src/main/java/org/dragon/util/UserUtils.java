package org.dragon.util;

import org.dragon.user.security.filter.JwtAuthenticationFilter;
import org.dragon.util.bean.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户信息获取类
 *
 * @author czj
 */
public class UserUtils {

    /**
     * 获取当前用户信息
     *
     * @return UserInfo 对象，如果未认证则返回空对象
     */
    public static UserInfo getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new UserInfo();
        }

        String userId;
        String username;

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtAuthenticationFilter.UserPrincipal(Long id, String username1)) {
            userId = String.valueOf(id);
            username = username1;
            return new UserInfo(userId, username, null, authentication.isAuthenticated());
        }

        return new UserInfo();
    }

    public static String getUserId() {
        return getUserInfo().getUserId();
    }

    public static String getUsername() {
        return getUserInfo().getUsername();
    }
}
