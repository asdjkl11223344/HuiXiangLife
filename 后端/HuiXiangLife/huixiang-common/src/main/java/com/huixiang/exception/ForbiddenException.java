package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class ForbiddenException extends BaseException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
