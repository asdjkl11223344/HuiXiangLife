package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.context.BaseContext;
import com.huixiang.entity.OperationLog;
import com.huixiang.entity.SysUser;
import com.huixiang.mapper.OperationLogMapper;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.query.OperationLogQuery;
import com.huixiang.service.OperationLogService;
import com.huixiang.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private static final Map<String, String> MODULE_NAME_MAP = Map.of(
            "product", "商品管理",
            "merchant", "商户管理",
            "merchant-category", "商户分类",
            "coupon-template", "券模板管理",
            "user", "用户管理",
            "review", "评价管理",
            "order", "订单管理",
            "auth", "认证管理"
    );

    private final OperationLogMapper operationLogMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void recordAdminOperation(HttpServletRequest request, HandlerMethod handlerMethod) {
        Long operatorId = BaseContext.getCurrentId();
        if (operatorId == null) {
            return;
        }
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setOperatorId(operatorId);
            operationLog.setModule(resolveModule(request));
            operationLog.setAction(resolveAction(request));
            operationLog.setBizId(resolveBizId(request));
            operationLog.setDetail(buildDetail(request, handlerMethod));
            operationLog.setIp(resolveClientIp(request));
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.warn("记录管理端操作日志失败, uri={}", request.getRequestURI(), e);
        }
    }

    @Override
    public Page<OperationLogVO> page(OperationLogQuery operationLogQuery) {
        Page<OperationLog> page = new Page<>(operationLogQuery.getPageNo(), operationLogQuery.getPageSize());
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (operationLogQuery.getOperatorId() != null) {
            queryWrapper.eq(OperationLog::getOperatorId, operationLogQuery.getOperatorId());
        }
        if (StringUtils.hasText(operationLogQuery.getModule())) {
            queryWrapper.like(OperationLog::getModule, operationLogQuery.getModule().trim());
        }
        if (StringUtils.hasText(operationLogQuery.getAction())) {
            queryWrapper.like(OperationLog::getAction, operationLogQuery.getAction().trim());
        }
        if (operationLogQuery.getBizId() != null) {
            queryWrapper.eq(OperationLog::getBizId, operationLogQuery.getBizId());
        }
        queryWrapper.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> operationLogPage = operationLogMapper.selectPage(page, queryWrapper);

        Map<Long, SysUser> userMap = loadUserMap(operationLogPage.getRecords());
        List<OperationLogVO> records = operationLogPage.getRecords().stream()
                .map(operationLog -> buildOperationLogVO(operationLog, userMap.get(operationLog.getOperatorId())))
                .toList();

        Page<OperationLogVO> resultPage = new Page<>(operationLogPage.getCurrent(), operationLogPage.getSize());
        resultPage.setTotal(operationLogPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    private Map<Long, SysUser> loadUserMap(List<OperationLog> records) {
        List<Long> operatorIds = records.stream()
                .map(OperationLog::getOperatorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (operatorIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysUser::getId, operatorIds);
        return sysUserMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private OperationLogVO buildOperationLogVO(OperationLog operationLog, SysUser sysUser) {
        OperationLogVO operationLogVO = new OperationLogVO();
        operationLogVO.setId(operationLog.getId());
        operationLogVO.setOperatorId(operationLog.getOperatorId());
        operationLogVO.setOperatorName(sysUser == null ? null : sysUser.getNickname());
        operationLogVO.setModule(operationLog.getModule());
        operationLogVO.setAction(operationLog.getAction());
        operationLogVO.setBizId(operationLog.getBizId());
        operationLogVO.setDetail(operationLog.getDetail());
        operationLogVO.setIp(operationLog.getIp());
        operationLogVO.setCreateTime(operationLog.getCreateTime());
        return operationLogVO;
    }

    private String resolveModule(HttpServletRequest request) {
        String[] segments = request.getRequestURI().split("/");
        if (segments.length < 3) {
            return "管理端";
        }
        return MODULE_NAME_MAP.getOrDefault(segments[2], segments[2]);
    }

    private String resolveAction(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            if (uri.endsWith("/search/rebuild")) {
                return "重建搜索索引";
            }
            if (uri.endsWith("/search/sync")) {
                return "同步搜索索引";
            }
            if (uri.endsWith("/seckill/preheat")) {
                return "预热秒杀库存";
            }
            if (uri.endsWith("/seckill/preheat/batch")) {
                return "批量预热秒杀库存";
            }
            if (uri.endsWith("/seckill/reset")) {
                return "重置秒杀状态";
            }
            if (uri.endsWith("/seckill/reset/batch")) {
                return "批量重置秒杀状态";
            }
            if (uri.endsWith("/seckill/preheat/trigger")) {
                return "触发秒杀预热";
            }
            if (uri.endsWith("/logout")) {
                return "退出登录";
            }
            return "新增";
        }
        if ("PUT".equalsIgnoreCase(method)) {
            if (uri.endsWith("/status")) {
                return "修改状态";
            }
            return "修改";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "删除";
        }
        return method;
    }

    private Long resolveBizId(HttpServletRequest request) {
        String[] segments = request.getRequestURI().split("/");
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != null && segments[i].matches("\\d+")) {
                try {
                    return Long.valueOf(segments[i]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String buildDetail(HttpServletRequest request, HandlerMethod handlerMethod) {
        return request.getMethod() + " " + request.getRequestURI() + " -> "
                + handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
