package org.dragon.user.dto;

import lombok.Data;

/**
 * 更新用户信息请求
 */
@Data
public class UpdateUserRequest {

    private String nickname;
    private String avatar;
}
