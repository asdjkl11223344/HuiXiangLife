package com.huixiang.constant;

public final class SecurityConstant {

    public static final String AUTHORIZATION_HEADER = "Authorization"; // 认证请求头名称
    public static final String TOKEN_TYPE = "Bearer"; // Token 类型
    public static final String TOKEN_PREFIX = "Bearer "; // Token 前缀
    public static final String USER_TOKEN_BLACKLIST_PREFIX = "auth:blacklist:user:token:"; // 用户 Token 黑名单前缀
    public static final String ADMIN_TOKEN_BLACKLIST_PREFIX = "auth:blacklist:admin:token:"; // 管理员 Token 黑名单前缀

    public static final String CURRENT_USER_ID = "currentUserId"; // 当前用户 ID 上下文键
    public static final String CURRENT_USER = "currentUser"; // 当前用户上下文键
    public static final String USER_ID_HEADER = "userId"; // 用户 ID 请求头名称

    private SecurityConstant() {
    }
}
