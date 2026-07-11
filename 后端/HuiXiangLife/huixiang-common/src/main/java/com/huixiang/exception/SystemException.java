package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class SystemException extends BaseException {

    public SystemException() {
        super(ErrorCode.SYSTEM_ERROR);
    }

    public SystemException(String message) {
        super(ErrorCode.SYSTEM_ERROR, message);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
