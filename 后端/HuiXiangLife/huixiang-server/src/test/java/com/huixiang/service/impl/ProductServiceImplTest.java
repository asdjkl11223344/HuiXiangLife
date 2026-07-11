package com.huixiang.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.ProductConstant;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.Product;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.query.ProductQuery;
import com.huixiang.service.ProductSearchIndexService;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.ProductDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private ProductSearchIndexService productSearchIndexService;
    @Mock
    private SeckillService seckillService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productMapper,
                merchantMapper,
                productSearchIndexService,
                seckillService,
                redisTemplate
        );
    }

    @Test
    void pageShouldFallbackToDatabaseWhenSearchIndexUnavailable() {
        ProductQuery query = new ProductQuery();
        query.setKeyword("周末");
        query.setPageNo(1);
        query.setPageSize(10);

        Product product = buildProduct(2001L, 1001L, "周末双人餐");
        Page<Product> databasePage = new Page<>(1, 10);
        databasePage.setTotal(1);
        databasePage.setRecords(List.of(product));

        when(productSearchIndexService.searchProductIdsByKeyword("周末", 1000)).thenReturn(null);
        when(productMapper.selectPage(any(Page.class), any())).thenReturn(databasePage);
        when(merchantMapper.selectById(1001L)).thenReturn(buildMerchant(1001L, "周末餐厅"));

        Page<ProductDetailVO> result = productService.page(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(2001L, result.getRecords().get(0).getId());
        verify(productMapper).selectPage(any(Page.class), any());
    }

    @Test
    void pageShouldRespectElasticSearchOrderAndPagination() {
        ProductQuery query = new ProductQuery();
        query.setKeyword("咖啡");
        query.setPageNo(1);
        query.setPageSize(1);

        Product product1 = buildProduct(2001L, 1001L, "咖啡套餐A");
        Product product2 = buildProduct(2002L, 1002L, "咖啡套餐B");

        when(productSearchIndexService.searchProductIdsByKeyword("咖啡", 1000)).thenReturn(List.of(2002L, 2001L));
        when(productMapper.selectList(any())).thenReturn(List.of(product1, product2));
        when(merchantMapper.selectById(1002L)).thenReturn(buildMerchant(1002L, "商户B"));

        Page<ProductDetailVO> result = productService.page(query);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(2002L, result.getRecords().get(0).getId());
        assertEquals("商户B", result.getRecords().get(0).getMerchantName());
        verify(productMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void pageShouldReturnEmptyPageWhenElasticSearchReturnsNoMatch() {
        ProductQuery query = new ProductQuery();
        query.setKeyword("不存在的关键词");
        query.setPageNo(1);
        query.setPageSize(10);

        when(productSearchIndexService.searchProductIdsByKeyword("不存在的关键词", 1000)).thenReturn(List.of());

        Page<ProductDetailVO> result = productService.page(query);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(productMapper, never()).selectList(any());
        verify(productMapper, never()).selectPage(any(Page.class), any());
    }

    private Product buildProduct(Long id, Long merchantId, String name) {
        Product product = new Product();
        product.setId(id);
        product.setMerchantId(merchantId);
        product.setName(name);
        product.setSalePrice(BigDecimal.valueOf(19.9));
        product.setStock(10);
        product.setSoldCount(5);
        product.setStatus(ProductConstant.STATUS_ON_SHELF);
        return product;
    }

    private Merchant buildMerchant(Long id, String name) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setName(name);
        return merchant;
    }
}
