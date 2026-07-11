package com.huixiang.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.MerchantConstant;
import com.huixiang.entity.Merchant;
import com.huixiang.mapper.MerchantCategoryMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.OrderInfoMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.query.MerchantQuery;
import com.huixiang.service.MerchantSearchIndexService;
import com.huixiang.vo.MerchantDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private MerchantCategoryMapper merchantCategoryMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private MerchantSearchIndexService merchantSearchIndexService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() {
        merchantService = new MerchantServiceImpl(
                merchantMapper,
                merchantCategoryMapper,
                productMapper,
                orderInfoMapper,
                merchantSearchIndexService,
                redisTemplate
        );
    }

    @Test
    void deleteShouldRemoveMerchantAndSearchIndexWhenNoAssociationExists() {
        Merchant merchant = buildMerchant(1002L, "瑞幸咖啡");
        when(merchantMapper.selectById(1002L)).thenReturn(merchant);
        when(productMapper.selectCount(any())).thenReturn(0L);
        when(orderInfoMapper.selectCount(any())).thenReturn(0L);

        Boolean result = merchantService.delete(1002L);

        assertTrue(result);
        verify(merchantMapper).deleteById(1002L);
        verify(merchantSearchIndexService).deleteMerchant(1002L);
    }

    @Test
    void deleteShouldBlockWhenMerchantStillHasProducts() {
        Merchant merchant = buildMerchant(1002L, "瑞幸咖啡");
        when(merchantMapper.selectById(1002L)).thenReturn(merchant);
        when(productMapper.selectCount(any())).thenReturn(2L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> merchantService.delete(1002L));

        assertEquals("该商户下仍有关联商品，不能删除", exception.getMessage());
        verify(merchantMapper, never()).deleteById(any());
    }

    @Test
    void deleteShouldBlockWhenMerchantStillHasOrders() {
        Merchant merchant = buildMerchant(1002L, "瑞幸咖啡");
        when(merchantMapper.selectById(1002L)).thenReturn(merchant);
        when(productMapper.selectCount(any())).thenReturn(0L);
        when(orderInfoMapper.selectCount(any())).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> merchantService.delete(1002L));

        assertEquals("该商户下仍有关联订单，不能删除", exception.getMessage());
        verify(merchantMapper, never()).deleteById(any());
    }

    @Test
    void pageShouldRespectElasticSearchOrderAndPagination() {
        MerchantQuery query = new MerchantQuery();
        query.setKeyword("连锁");
        query.setPageNo(1);
        query.setPageSize(1);

        Merchant merchant1 = buildMerchant(1001L, "门店A");
        Merchant merchant2 = buildMerchant(1002L, "门店B");
        when(merchantSearchIndexService.searchMerchantIdsByKeyword("连锁", 1000)).thenReturn(List.of(1002L, 1001L));
        when(merchantMapper.selectList(any())).thenReturn(List.of(merchant1, merchant2));

        Page<MerchantDetailVO> result = merchantService.page(query);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(1002L, result.getRecords().get(0).getId());
        assertEquals("门店B", result.getRecords().get(0).getName());
    }

    private Merchant buildMerchant(Long id, String name) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setName(name);
        merchant.setStatus(MerchantConstant.STATUS_ENABLED);
        merchant.setAvgPrice(BigDecimal.valueOf(29.9));
        return merchant;
    }
}
