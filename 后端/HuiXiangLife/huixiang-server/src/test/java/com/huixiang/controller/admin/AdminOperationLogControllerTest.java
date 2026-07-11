package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.service.OperationLogService;
import com.huixiang.vo.OperationLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOperationLogControllerTest {

    @Mock
    private OperationLogService operationLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOperationLogController(operationLogService)).build();
    }

    @Test
    void pageShouldBindQueryAndReturnPageResult() throws Exception {
        OperationLogVO operationLogVO = new OperationLogVO();
        operationLogVO.setId(11L);
        operationLogVO.setModule("商户管理");
        operationLogVO.setAction("删除");
        operationLogVO.setOperatorName("系统管理员");
        operationLogVO.setCreateTime(LocalDateTime.of(2026, 6, 26, 18, 0, 0));

        Page<OperationLogVO> page = new Page<>(2, 5);
        page.setTotal(9);
        page.setRecords(List.of(operationLogVO));
        when(operationLogService.page(any())).thenReturn(page);

        mockMvc.perform(get("/admin/operation-log/page")
                        .param("pageNo", "2")
                        .param("pageSize", "5")
                        .param("module", "商户管理")
                        .param("action", "删除")
                        .param("bizId", "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data.current").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(9))
                .andExpect(jsonPath("$.data.records[0].module").value("商户管理"))
                .andExpect(jsonPath("$.data.records[0].action").value("删除"));

        ArgumentCaptor<com.huixiang.query.OperationLogQuery> captor =
                ArgumentCaptor.forClass(com.huixiang.query.OperationLogQuery.class);
        verify(operationLogService).page(captor.capture());
        assertEquals(2, captor.getValue().getPageNo());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals("商户管理", captor.getValue().getModule());
        assertEquals("删除", captor.getValue().getAction());
        assertEquals(1002L, captor.getValue().getBizId());
    }
}
