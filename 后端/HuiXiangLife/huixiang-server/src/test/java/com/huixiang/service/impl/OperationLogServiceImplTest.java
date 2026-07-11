package com.huixiang.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.context.BaseContext;
import com.huixiang.controller.admin.AdminMerchantController;
import com.huixiang.entity.OperationLog;
import com.huixiang.entity.SysUser;
import com.huixiang.mapper.OperationLogMapper;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.query.OperationLogQuery;
import com.huixiang.service.MerchantService;
import com.huixiang.vo.OperationLogVO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationLogServiceImplTest {

    @Mock
    private OperationLogMapper operationLogMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private HttpServletRequest request;
    @Mock
    private MerchantService merchantService;

    private OperationLogServiceImpl operationLogService;

    @BeforeEach
    void setUp() {
        operationLogService = new OperationLogServiceImpl(operationLogMapper, sysUserMapper);
        BaseContext.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void recordAdminOperationShouldInsertResolvedFields() throws NoSuchMethodException {
        Method method = AdminMerchantController.class.getMethod("syncSearchIndex", Long.class);
        HandlerMethod handlerMethod = new HandlerMethod(new AdminMerchantController(merchantService), method);
        when(request.getRequestURI()).thenReturn("/admin/merchant/1002/search/sync");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.8, 10.0.0.9");

        operationLogService.recordAdminOperation(request, handlerMethod);

        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(operationLogMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getOperatorId());
        assertEquals("商户管理", captor.getValue().getModule());
        assertEquals("同步搜索索引", captor.getValue().getAction());
        assertEquals(1002L, captor.getValue().getBizId());
        assertEquals("10.0.0.8", captor.getValue().getIp());
        assertTrue(captor.getValue().getDetail().contains("AdminMerchantController#syncSearchIndex"));
    }

    @Test
    void pageShouldAssembleOperatorName() {
        OperationLogQuery query = new OperationLogQuery();
        query.setPageNo(1);
        query.setPageSize(10);

        OperationLog operationLog = new OperationLog();
        operationLog.setId(10L);
        operationLog.setOperatorId(1L);
        operationLog.setModule("商户管理");
        operationLog.setAction("同步搜索索引");
        operationLog.setBizId(1002L);
        operationLog.setDetail("POST /admin/merchant/1002/search/sync -> AdminMerchantController#syncSearchIndex");
        operationLog.setIp("127.0.0.1");
        operationLog.setCreateTime(LocalDateTime.of(2026, 6, 26, 11, 46, 27));

        Page<OperationLog> logPage = new Page<>(1, 10);
        logPage.setTotal(1);
        logPage.setRecords(List.of(operationLog));

        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setNickname("系统管理员");

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(logPage);
        when(sysUserMapper.selectList(any())).thenReturn(List.of(sysUser));

        Page<OperationLogVO> result = operationLogService.page(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("系统管理员", result.getRecords().get(0).getOperatorName());
        assertEquals("商户管理", result.getRecords().get(0).getModule());
        assertEquals("同步搜索索引", result.getRecords().get(0).getAction());
    }

    @Test
    void pageShouldReturnNullOperatorNameWhenUserMissing() {
        OperationLogQuery query = new OperationLogQuery();
        OperationLog operationLog = new OperationLog();
        operationLog.setId(11L);
        operationLog.setOperatorId(99L);

        Page<OperationLog> logPage = new Page<>(1, 10);
        logPage.setTotal(1);
        logPage.setRecords(List.of(operationLog));

        when(operationLogMapper.selectPage(any(Page.class), any())).thenReturn(logPage);
        when(sysUserMapper.selectList(any())).thenReturn(List.of());

        Page<OperationLogVO> result = operationLogService.page(query);

        assertEquals(1, result.getTotal());
        assertNull(result.getRecords().get(0).getOperatorName());
    }
}
