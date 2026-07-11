package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(ErrorCode.BUSINESS_ERROR, message);
    }

    public BusinessException(Integer code, String message) {
        super(code, message);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
