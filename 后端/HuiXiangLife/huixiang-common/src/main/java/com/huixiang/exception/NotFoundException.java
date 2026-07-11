package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class NotFoundException extends BaseException {

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
