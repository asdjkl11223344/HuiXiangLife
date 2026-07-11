package com.huixiang.handler;

import com.huixiang.exception.BaseException;
import com.huixiang.enumeration.ErrorCode;
import com.huixiang.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException exception) {
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数校验失败";
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数绑定失败";
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), message.isEmpty() ? "请求参数校验失败" : message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException exception) {
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), "缺少请求参数：" + exception.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exception) {
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), "参数类型错误：" + exception.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return Result.error(ErrorCode.PARAM_INVALID.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(), "不支持的请求方法：" + exception.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exception) {
        return Result.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode(), "不支持的请求类型");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException exception) {
        log.warn("数据库唯一键冲突", exception);
        return Result.error(ErrorCode.DUPLICATE_KEY.getCode(), ErrorCode.DUPLICATE_KEY.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
