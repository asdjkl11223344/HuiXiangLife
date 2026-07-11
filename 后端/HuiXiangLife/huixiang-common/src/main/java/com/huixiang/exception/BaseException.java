package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BaseException(String message) {
        this(ErrorCode.BUSINESS_ERROR, message);
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BaseException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public Integer getCode() {
        return code;
    }
}
