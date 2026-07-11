package com.huixiang.enumeration;

public enum ErrorCode {

    SYSTEM_ERROR(0, "服务器异常，请稍后重试"),
    SUCCESS(1, "success"),
    BUSINESS_ERROR(1000, "业务处理失败"),
    PARAM_INVALID(1001, "请求参数不合法"),
    UNAUTHORIZED(1002, "请先登录"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "请求资源不存在"),
    METHOD_NOT_ALLOWED(1005, "请求方法不支持"),
    UNSUPPORTED_MEDIA_TYPE(1006, "请求类型不支持"),
    DATA_CONFLICT(1007, "数据冲突"),
    DUPLICATE_KEY(1008, "数据已存在");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
