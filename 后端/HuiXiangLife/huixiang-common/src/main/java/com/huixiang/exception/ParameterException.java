package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class ParameterException extends BaseException {

    public ParameterException() {
        super(ErrorCode.PARAM_INVALID);
    }

    public ParameterException(String message) {
        super(ErrorCode.PARAM_INVALID, message);
    }
}
