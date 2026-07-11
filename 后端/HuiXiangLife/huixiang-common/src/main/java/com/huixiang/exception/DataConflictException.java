package com.huixiang.exception;

import com.huixiang.enumeration.ErrorCode;

public class DataConflictException extends BaseException {

    public DataConflictException() {
        super(ErrorCode.DATA_CONFLICT);
    }

    public DataConflictException(String message) {
        super(ErrorCode.DATA_CONFLICT, message);
    }
}
