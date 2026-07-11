package com.huixiang.controller.admin;

import com.huixiang.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMerchantControllerTest {

    @Mock
    private MerchantService merchantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMerchantController(merchantService)).build();
    }

    @Test
    void deleteShouldReturnSuccessResult() throws Exception {
        when(merchantService.delete(1002L)).thenReturn(true);

        mockMvc.perform(delete("/admin/merchant/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data").value(true));

        verify(merchantService).delete(1002L);
    }

    @Test
    void rebuildSearchIndexShouldReturnAffectedCount() throws Exception {
        when(merchantService.rebuildSearchIndex()).thenReturn(2);

        mockMvc.perform(post("/admin/merchant/search/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.msg").value("success"))
                .andExpect(jsonPath("$.data").value(2));

        verify(merchantService).rebuildSearchIndex();
    }
}
