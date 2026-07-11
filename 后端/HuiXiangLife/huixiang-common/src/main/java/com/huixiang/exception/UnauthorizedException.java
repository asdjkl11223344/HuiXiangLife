package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
