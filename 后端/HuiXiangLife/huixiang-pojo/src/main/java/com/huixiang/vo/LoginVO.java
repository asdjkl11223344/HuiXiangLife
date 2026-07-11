package com.huixiang.vo;

import lombok.Data;

@Data
public class LoginVO {

    /**
     * 登录令牌
     */
    private String token;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 过期时间，单位毫秒
     */
    private Long expireIn;

    /**
     * 当前登录用户信息
     */
    private UserInfoVO userInfo;
}