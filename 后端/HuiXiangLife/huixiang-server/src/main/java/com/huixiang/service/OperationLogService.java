package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.query.OperationLogQuery;
import com.huixiang.vo.OperationLogVO;
import org.springframework.web.method.HandlerMethod;

import jakarta.servlet.http.HttpServletRequest;

public interface OperationLogService {

    /**
     * 记录管理端操作日志
     */
    void recordAdminOperation(HttpServletRequest request, HandlerMethod handlerMethod);

    /**
     * 分页查询操作日志
     */
    Page<OperationLogVO> page(OperationLogQuery operationLogQuery);
}
