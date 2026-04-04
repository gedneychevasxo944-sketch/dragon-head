package org.dragon.util.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息对象
 *
 * @author czj
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 是否已认证
     */
    private boolean authenticated;
}
