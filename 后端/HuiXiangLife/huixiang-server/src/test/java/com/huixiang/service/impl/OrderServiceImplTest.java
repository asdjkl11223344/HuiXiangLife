package com.huixiang.service.impl;

import com.huixiang.constant.ProductConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.dto.SeckillOrderCreateDTO;
import com.huixiang.entity.Product;
import com.huixiang.exception.BusinessException;
import com.huixiang.mapper.CouponTemplateMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.MqConsumeLogMapper;
import com.huixiang.mapper.OrderInfoMapper;
import com.huixiang.mapper.OrderItemMapper;
import com.huixiang.mapper.PaymentRecordMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.mapper.RefundRecordMapper;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.mapper.UserCouponMapper;
import com.huixiang.service.MqMessageService;
import com.huixiang.service.ProductService;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.SeckillSubmitVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private CouponTemplateMapper couponTemplateMapper;
    @Mock
    private PaymentRecordMapper paymentRecordMapper;
    @Mock
    private RefundRecordMapper refundRecordMapper;
    @Mock
    private MqConsumeLogMapper mqConsumeLogMapper;
    @Mock
    private ProductService productService;
    @Mock
    private MqMessageService mqMessageService;
    @Mock
    private SeckillService seckillService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderInfoMapper,
                orderItemMapper,
                productMapper,
                merchantMapper,
                sysUserMapper,
                userCouponMapper,
                couponTemplateMapper,
                paymentRecordMapper,
                refundRecordMapper,
                mqConsumeLogMapper,
                productService,
                mqMessageService,
                seckillService,
                redisTemplate
        );
        BaseContext.setCurrentId(88L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void createSeckillShouldReturnSubmitInfoWhenPreDeductSucceeds() {
        Product product = buildValidSeckillProduct();
        SeckillOrderCreateDTO dto = buildSeckillOrderCreateDTO();
        when(productMapper.selectById(2001L)).thenReturn(product);
        when(seckillService.tryPreDeduct(eq(88L), eq(product), anyString())).thenReturn(SeckillService.PRE_DEDUCT_SUCCESS);

        SeckillSubmitVO result = orderService.createSeckill(dto);

        assertNotNull(result.getRequestId());
        assertEquals(SeckillService.POLL_INTERVAL_PENDING_MILLIS, result.getPollIntervalMillis());
        assertEquals("抢购请求已提交，请尽快支付", result.getMessage());

        ArgumentCaptor<AsyncOrderCreateDTO> captor = ArgumentCaptor.forClass(AsyncOrderCreateDTO.class);
        verify(mqMessageService).sendAsyncOrderCreateMessage(captor.capture());
        assertEquals(88L, captor.getValue().getUserId());
        assertEquals(1001L, captor.getValue().getMerchantId());
        assertEquals(2001L, captor.getValue().getProductId());
        assertEquals("测试秒杀", captor.getValue().getRemark());
        verify(seckillService, never()).rollbackPreDeduct(88L, 2001L);
    }

    @Test
    void createSeckillShouldExposeRepeatFailureCode() {
        Product product = buildValidSeckillProduct();
        SeckillOrderCreateDTO dto = buildSeckillOrderCreateDTO();
        when(productMapper.selectById(2001L)).thenReturn(product);
        when(seckillService.tryPreDeduct(eq(88L), eq(product), anyString())).thenReturn(SeckillService.PRE_DEDUCT_REPEAT);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createSeckill(dto));

        assertEquals(SeckillService.FAILURE_CODE_REPEAT, exception.getCode());
        assertEquals("该商品已提交抢购，请勿重复操作", exception.getMessage());
        verify(mqMessageService, never()).sendAsyncOrderCreateMessage(any());
    }

    @Test
    void createSeckillShouldRollbackPreDeductWhenAsyncMessageFails() {
        Product product = buildValidSeckillProduct();
        SeckillOrderCreateDTO dto = buildSeckillOrderCreateDTO();
        when(productMapper.selectById(2001L)).thenReturn(product);
        when(seckillService.tryPreDeduct(eq(88L), eq(product), anyString())).thenReturn(SeckillService.PRE_DEDUCT_SUCCESS);
        doThrow(new RuntimeException("mq unavailable")).when(mqMessageService).sendAsyncOrderCreateMessage(any(AsyncOrderCreateDTO.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.createSeckill(dto));

        assertEquals("mq unavailable", exception.getMessage());
        verify(seckillService).rollbackPreDeduct(88L, 2001L);
    }

    @Test
    void createSeckillShouldExposeActivityNotStartedCode() {
        Product product = buildValidSeckillProduct();
        product.setStartTime(LocalDateTime.now().plusMinutes(5));
        SeckillOrderCreateDTO dto = buildSeckillOrderCreateDTO();
        when(productMapper.selectById(2001L)).thenReturn(product);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.createSeckill(dto));

        assertEquals(SeckillService.FAILURE_CODE_ACTIVITY_NOT_STARTED, exception.getCode());
        assertEquals("秒杀活动未开始", exception.getMessage());
        verify(seckillService, never()).tryPreDeduct(any(), any(), anyString());
    }

    private Product buildValidSeckillProduct() {
        Product product = new Product();
        product.setId(2001L);
        product.setMerchantId(1001L);
        product.setStatus(ProductConstant.STATUS_ON_SHELF);
        product.setStartTime(LocalDateTime.now().minusMinutes(5));
        product.setEndTime(LocalDateTime.now().plusMinutes(30));
        return product;
    }

    private SeckillOrderCreateDTO buildSeckillOrderCreateDTO() {
        SeckillOrderCreateDTO dto = new SeckillOrderCreateDTO();
        dto.setMerchantId(1001L);
        dto.setProductId(2001L);
        dto.setRemark("测试秒杀");
        return dto;
    }
}
