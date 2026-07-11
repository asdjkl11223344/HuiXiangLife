package com.huixiang.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserInfoVO {

    /**
     * 用户ID
     */
    private Long id;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 角色
     */
    private String role;
    /**
     * 账号状态
     */
    private Integer status;
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}
