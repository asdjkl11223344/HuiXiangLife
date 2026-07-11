package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.*;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.AdminOrderRefundDTO;
import com.huixiang.dto.AsyncOrderCreateDTO;
import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.dto.OrderCreateDTO;
import com.huixiang.dto.PaymentCreateDTO;
import com.huixiang.dto.SeckillOrderCreateDTO;
import com.huixiang.entity.*;
import com.huixiang.exception.BaseException;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.mapper.*;
import com.huixiang.query.OrderQuery;
import com.huixiang.service.MqMessageService;
import com.huixiang.service.OrderService;
import com.huixiang.service.ProductService;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.OrderDetailVO;
import com.huixiang.vo.PaymentSubmitVO;
import com.huixiang.vo.SeckillResultVO;
import com.huixiang.vo.SeckillSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final SysUserMapper sysUserMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final ProductService productService;
    private final MqMessageService mqMessageService;
    private final SeckillService seckillService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public Long create(OrderCreateDTO orderCreateDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        return createOrder(currentId, orderCreateDTO, true);
    }

    @Override
    public SeckillSubmitVO createSeckill(SeckillOrderCreateDTO seckillOrderCreateDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        Product product = productMapper.selectById(seckillOrderCreateDTO.getProductId());
        validateSeckillProduct(product, seckillOrderCreateDTO.getMerchantId());
        String requestId = IdWorker.getIdStr();
        int preDeductResult = seckillService.tryPreDeduct(currentId, product, requestId);
        if (preDeductResult == SeckillService.PRE_DEDUCT_REPEAT) {
            throw new BusinessException(SeckillService.FAILURE_CODE_REPEAT, "该商品已提交抢购，请勿重复操作");
        }
        if (preDeductResult == SeckillService.PRE_DEDUCT_SOLD_OUT) {
            throw new BusinessException(SeckillService.FAILURE_CODE_SOLD_OUT, "商品已抢完");
        }
        if (preDeductResult != SeckillService.PRE_DEDUCT_SUCCESS) {
            throw new BusinessException(SeckillService.FAILURE_CODE_SYSTEM_BUSY, "抢购人数过多，请稍后重试");
        }
        AsyncOrderCreateDTO asyncOrderCreateDTO = buildAsyncOrderCreateDTO(currentId, seckillOrderCreateDTO, requestId);
        boolean orderCreated = false;
        try {
            mqMessageService.sendAsyncOrderCreateMessage(asyncOrderCreateDTO);
            SeckillSubmitVO seckillSubmitVO = new SeckillSubmitVO();
            seckillSubmitVO.setRequestId(requestId);
            seckillSubmitVO.setPollIntervalMillis(SeckillService.POLL_INTERVAL_PENDING_MILLIS);
            seckillSubmitVO.setMessage("抢购请求已提交，请尽快支付");
            orderCreated = true;
            return seckillSubmitVO;
        } finally {
            if (!orderCreated) {
                seckillService.rollbackPreDeduct(currentId, seckillOrderCreateDTO.getProductId());
            }
        }
    }

    @Override
    public SeckillResultVO getSeckillResult(Long productId) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        if (productId == null) {
            throw new ParameterException("商品ID不能为空");
        }
        return seckillService.getResult(currentId, productId);
    }

    @Override
    @Transactional
    public void createSeckillOrderAsync(AsyncOrderCreateDTO asyncOrderCreateDTO) {
        validateAsyncOrderCreateDTO(asyncOrderCreateDTO);
        if (hasConsumedAsyncOrderMessage(asyncOrderCreateDTO.getMessageId())) {
            return;
        }
        Long orderId;
        try {
            orderId = createOrder(asyncOrderCreateDTO.getUserId(), buildSeckillOrderCreateOrderDTO(asyncOrderCreateDTO), false);
        } catch (Exception e) {
            seckillService.rollbackPreDeduct(
                    asyncOrderCreateDTO.getUserId(),
                    asyncOrderCreateDTO.getProductId(),
                    resolveSeckillFailureCode(e),
                    resolveSeckillFailureMessage(e)
            );
            throw e;
        }
        saveAsyncOrderConsumeLog(asyncOrderCreateDTO, orderId);
        registerSeckillOrderSuccessAfterCommit(orderId, asyncOrderCreateDTO.getUserId(), asyncOrderCreateDTO.getProductId());
    }

    @Override
    public Page<OrderDetailVO> page(OrderQuery orderQuery) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        String orderPageCacheKey = null;
        if (shouldUseOrderPageCache(orderQuery)) {
            orderPageCacheKey = buildOrderPageCacheKey(currentId, orderQuery);
            Page<OrderDetailVO> cachePage = getOrderPageCache(orderPageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<OrderInfo> page = new Page<>(orderQuery.getPageNo(), orderQuery.getPageSize());
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getUserId, currentId);
        if (StringUtils.hasText(orderQuery.getOrderNo())) {
            queryWrapper.like(OrderInfo::getOrderNo, orderQuery.getOrderNo());
        }
        if (orderQuery.getMerchantId() != null) {
            queryWrapper.eq(OrderInfo::getMerchantId, orderQuery.getMerchantId());
        }
        if (orderQuery.getProductId() != null) {
            queryWrapper.eq(OrderInfo::getProductId, orderQuery.getProductId());
        }
        if (orderQuery.getStatus() != null) {
            queryWrapper.eq(OrderInfo::getStatus, orderQuery.getStatus());
        }
        if (orderQuery.getBeginCreateTime() != null) {
            queryWrapper.ge(OrderInfo::getCreateTime, orderQuery.getBeginCreateTime());
        }
        if (orderQuery.getEndCreateTime() != null) {
            queryWrapper.le(OrderInfo::getCreateTime, orderQuery.getEndCreateTime());
        }
        queryWrapper.orderByDesc(OrderInfo::getCreateTime);
        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, queryWrapper);
        List<OrderDetailVO> records = orderPage.getRecords()
                .stream()
                .map(this::buildOrderDetailVO)
                .toList();
        Page<OrderDetailVO> resultPage = new Page<>(
                orderPage.getCurrent(),
                orderPage.getSize()
        );
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setRecords(records);
        if (orderPageCacheKey != null) {
            long ttl = CacheConstant.ORDER_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.ORDER_PAGE_TTL_RANDOM_BOUND_MINUTES + 1);
            setOrderPageCache(orderPageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    public OrderDetailVO detail(Long id) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        String orderDetailCacheKey = buildOrderDetailCacheKey(currentId, id);
        OrderDetailVO cacheDetail = getOrderDetailCache(orderDetailCacheKey);
        if (cacheDetail != null) {
            return cacheDetail;
        }
        OrderInfo userOrder = getUserOrder(id, currentId);
        OrderDetailVO orderDetailVO = buildOrderDetailVO(userOrder);
        long ttl = CacheConstant.ORDER_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.ORDER_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setOrderDetailCache(orderDetailCacheKey, orderDetailVO, ttl);
        return orderDetailVO;
    }

    @Override
    @Transactional
    public Boolean cancel(Long id) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        OrderInfo orderInfo = getUserOrder(id, currentId);
        if (!OrderConstant.STATUS_PENDING_PAY.equals(orderInfo.getStatus())){
            throw new BusinessException("当前订单状态不可取消");
        }
        LocalDateTime cancelTime = LocalDateTime.now();
        cancelPendingOrder(orderInfo.getId(), cancelTime);
        orderInfo.setStatus(OrderConstant.STATUS_CANCELED);
        orderInfo.setCancelTime(cancelTime);
        OrderItem orderItem = getOrderItem(orderInfo.getId());
        if (orderItem!=null){
            int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int rows = productMapper.increaseStock(orderItem.getProductId(), quantity);
            if (rows > 0) {
                productService.evictProductReadCaches(orderItem.getProductId());
                seckillService.restoreStockForCanceledOrder(orderInfo.getId(), orderItem.getProductId());
            }
        }
        if (orderInfo.getCouponId()!=null){
            UserCoupon userCoupon = userCouponMapper.selectById(orderInfo.getCouponId());
            if (userCoupon!=null){
                releaseCouponLock(userCoupon, orderInfo.getId());
            }
        }
        deleteOrderDetailCache(currentId, orderInfo.getId());
        deleteAdminOrderDetailCache(orderInfo.getId());
        deleteOrderPageCache(currentId);
        return true;
    }

    @Override
    @Transactional
    public PaymentSubmitVO pay(Long id, PaymentCreateDTO paymentCreateDTO) {
        if (!id.equals(paymentCreateDTO.getOrderId())){
            throw new ParameterException("订单ID不一致");
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId==null){
            throw new UnauthorizedException("请先登录");
        }
        String paySubmitKey = buildOrderPaySubmitKey(currentId, paymentCreateDTO);
        if (!tryAcquireOrderPaySubmitLock(paySubmitKey)) {
            throw new BusinessException("支付请求正在处理，请勿重复提交");
        }
        boolean paySubmitted = false;
        try {
            OrderInfo orderInfo = orderInfoMapper.selectById(id);
            if (orderInfo==null||!currentId.equals(orderInfo.getUserId())){
                throw new NotFoundException("订单不存在");
            }
            if (!OrderConstant.STATUS_PENDING_PAY.equals(orderInfo.getStatus())) {
                throw new BusinessException("当前订单状态不可支付");
            }
            LambdaQueryWrapper<PaymentRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PaymentRecord::getOrderId, id)
                    .eq(PaymentRecord::getPayStatus, PaymentConstant.STATUS_PENDING)
                    .orderByDesc(PaymentRecord::getCreateTime)
                    .last("limit 1");
            PaymentRecord paymentRecord = paymentRecordMapper.selectOne(queryWrapper);
            if (paymentRecord == null) {
                PaymentRecord newRecord = new PaymentRecord();
                LocalDateTime now = LocalDateTime.now();
                newRecord.setId(IdWorker.getId());
                newRecord.setOrderId(id);
                newRecord.setPayChannel(paymentCreateDTO.getPayChannel());
                newRecord.setPayStatus(PaymentConstant.STATUS_PENDING);
                newRecord.setTransactionNo(generateTransactionNo());
                newRecord.setDeleted(0);
                newRecord.setCreateTime(now);
                newRecord.setUpdateTime(now);
                int rows = paymentRecordMapper.insertPendingIfAbsent(newRecord);
                if (rows > 0) {
                    paymentRecord = newRecord;
                } else {
                    paymentRecord = paymentRecordMapper.selectOne(queryWrapper);
                }
            }
            if (paymentRecord == null) {
                throw new BusinessException("支付记录创建失败，请稍后重试");
            }
            if (!paymentCreateDTO.getPayChannel().equals(paymentRecord.getPayChannel())) {
                refreshPendingPayment(paymentRecord.getId(), paymentCreateDTO.getPayChannel());
                paymentRecord = paymentRecordMapper.selectById(paymentRecord.getId());
            }
            PaymentSubmitVO paymentSubmitVO = new PaymentSubmitVO();
            paymentSubmitVO.setPaymentId(paymentRecord.getId());
            paymentSubmitVO.setOrderId(paymentRecord.getOrderId());
            paymentSubmitVO.setPayChannel(paymentRecord.getPayChannel());
            paymentSubmitVO.setPayStatus(paymentRecord.getPayStatus());
            paymentSubmitVO.setPayUrl("https://pay.example.com/prepay/" + paymentRecord.getTransactionNo());
            paymentSubmitVO.setTransactionNo(paymentRecord.getTransactionNo());
            paySubmitted = true;
            return paymentSubmitVO;
        } finally {
            if (!paySubmitted) {
                deleteOrderPaySubmitLock(paySubmitKey);
            }
        }
    }

    @Override
    public Page<OrderDetailVO> adminPage(OrderQuery orderQuery) {
        Page<OrderInfo> page = new Page<>(orderQuery.getPageNo(), orderQuery.getPageSize());

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(orderQuery.getOrderNo())) {
            queryWrapper.like(OrderInfo::getOrderNo, orderQuery.getOrderNo());
        }
        if (orderQuery.getUserId() != null) {
            queryWrapper.eq(OrderInfo::getUserId, orderQuery.getUserId());
        }
        if (orderQuery.getMerchantId() != null) {
            queryWrapper.eq(OrderInfo::getMerchantId, orderQuery.getMerchantId());
        }
        if (orderQuery.getProductId() != null) {
            queryWrapper.eq(OrderInfo::getProductId, orderQuery.getProductId());
        }
        if (orderQuery.getStatus() != null) {
            queryWrapper.eq(OrderInfo::getStatus, orderQuery.getStatus());
        }
        if (orderQuery.getBeginCreateTime() != null) {
            queryWrapper.ge(OrderInfo::getCreateTime, orderQuery.getBeginCreateTime());
        }
        if (orderQuery.getEndCreateTime() != null) {
            queryWrapper.le(OrderInfo::getCreateTime, orderQuery.getEndCreateTime());
        }
        if (orderQuery.getBeginPayTime() != null) {
            queryWrapper.ge(OrderInfo::getPayTime, orderQuery.getBeginPayTime());
        }
        if (orderQuery.getEndPayTime() != null) {
            queryWrapper.le(OrderInfo::getPayTime, orderQuery.getEndPayTime());
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        Page<OrderInfo> orderPage = orderInfoMapper.selectPage(page, queryWrapper);

        List<OrderDetailVO> records = orderPage.getRecords()
                .stream()
                .map(this::buildOrderDetailVO)
                .toList();

        Page<OrderDetailVO> resultPage = new Page<>(
                orderPage.getCurrent(),
                orderPage.getSize()
        );
        resultPage.setTotal(orderPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public OrderDetailVO adminDetail(Long id) {
        String key = buildAdminOrderDetailCacheKey(id);
        OrderDetailVO cacheOrderDetail = getAdminOrderDetailCache(key);
        if (cacheOrderDetail != null) {
            return cacheOrderDetail;
        }
        OrderInfo orderInfo = orderInfoMapper.selectById(id);
        if (orderInfo == null){
            throw new NotFoundException("订单不存在");
        }
        OrderDetailVO orderDetailVO = buildOrderDetailVO(orderInfo);
        long ttl = CacheConstant.ADMIN_ORDER_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.ADMIN_ORDER_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setAdminOrderDetailCache(key, orderDetailVO, ttl);
        return orderDetailVO;
    }

    @Override
    @Transactional
    public Boolean adminCancel(Long id) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);
        if (orderInfo == null){
            throw new NotFoundException("订单不存在");
        }
        if (!OrderConstant.STATUS_PENDING_PAY.equals(orderInfo.getStatus())){
            throw new BusinessException("当前订单状态不可取消");
        }
        LocalDateTime cancelTime = LocalDateTime.now();
        cancelPendingOrder(orderInfo.getId(), cancelTime);
        orderInfo.setStatus(OrderConstant.STATUS_CANCELED);
        orderInfo.setCancelTime(cancelTime);
        OrderItem orderItem = getOrderItem(orderInfo.getId());
        if (orderItem!=null){
            int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int rows = productMapper.increaseStock(orderItem.getProductId(), quantity);
            if (rows > 0) {
                productService.evictProductReadCaches(orderItem.getProductId());
                seckillService.restoreStockForCanceledOrder(orderInfo.getId(), orderItem.getProductId());
            }
        }
        if (orderInfo.getCouponId()!=null){
            UserCoupon userCoupon = userCouponMapper.selectById(orderInfo.getCouponId());
            if (userCoupon!=null){
                releaseCouponLock(userCoupon, orderInfo.getId());
            }
        }
        deleteOrderDetailCache(orderInfo.getUserId(), orderInfo.getId());
        deleteAdminOrderDetailCache(orderInfo.getId());
        deleteOrderPageCache(orderInfo.getUserId());
        return true;
    }

    @Override
    @Transactional
    public Boolean adminRefund(Long id, AdminOrderRefundDTO refundDTO) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);
        if (orderInfo == null){
            throw new NotFoundException("订单不存在");
        }
        if (!OrderConstant.STATUS_PAID.equals(orderInfo.getStatus())
                && !OrderConstant.STATUS_FINISHED.equals(orderInfo.getStatus())) {
            throw new BusinessException("当前订单状态不可退款");
        }
        if (refundDTO.getRefundAmount().compareTo(orderInfo.getPayAmount())>0){
            throw new BusinessException("退款金额不能大于实付金额");
        }
        LambdaQueryWrapper<RefundRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RefundRecord::getOrderId, id);
        Long count = refundRecordMapper.selectCount(queryWrapper);
        if (count!=null&&count>0){
            throw new BusinessException("该订单已发起退款");
        }
        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setOrderId(id);
        refundRecord.setRefundNo(generateRefundNo());
        refundRecord.setRefundAmount(refundDTO.getRefundAmount());
        refundRecord.setRefundStatus(RefundConstant.STATUS_SUCCESS);
        refundRecord.setReason(refundDTO.getReason());
        refundRecord.setApplyTime(LocalDateTime.now());
        refundRecord.setRefundTime(LocalDateTime.now());
        refundRecordMapper.insert(refundRecord);
        orderInfo.setStatus(OrderConstant.STATUS_REFUNDED);
        orderInfoMapper.updateById(orderInfo);
        deleteOrderDetailCache(orderInfo.getUserId(), orderInfo.getId());
        deleteAdminOrderDetailCache(orderInfo.getId());
        deleteOrderPageCache(orderInfo.getUserId());
        return true;
    }

    private Long createOrder(Long currentId, OrderCreateDTO orderCreateDTO, boolean useSubmitLock) {
        String orderSubmitKey = null;
        if (useSubmitLock) {
            orderSubmitKey = buildOrderSubmitKey(currentId, orderCreateDTO);
            if (!tryAcquireOrderSubmitLock(orderSubmitKey)) {
                throw new BusinessException("下单请求正在处理，请勿重复提交");
            }
        }
        boolean orderCreated = false;
        try {
            SysUser sysUser = sysUserMapper.selectById(currentId);
            if (sysUser == null) {
                throw new NotFoundException("用户不存在");
            }
            Product product = productMapper.selectById(orderCreateDTO.getProductId());
            if (product == null || !ProductConstant.STATUS_ON_SHELF.equals(product.getStatus())) {
                throw new NotFoundException("商品不存在");
            }
            if (!product.getMerchantId().equals(orderCreateDTO.getMerchantId())) {
                throw new ParameterException("商户信息不正确");
            }
            BigDecimal totalAmount = product.getSalePrice().multiply(BigDecimal.valueOf(orderCreateDTO.getQuantity()));
            BigDecimal discountAmount = BigDecimal.ZERO;
            UserCoupon userCoupon = null;
            CouponTemplate couponTemplate = null;
            if (orderCreateDTO.getCouponId() != null) {
                userCoupon = userCouponMapper.selectById(orderCreateDTO.getCouponId());
                if (userCoupon == null || !currentId.equals(userCoupon.getUserId())) {
                    throw new NotFoundException("优惠券不存在");
                }
                if (!CouponConstant.USER_COUPON_UNUSED.equals(userCoupon.getStatus())) {
                    throw new NotFoundException("优惠券不可用");
                }
                if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(LocalDateTime.now())) {
                    throw new NotFoundException("优惠券已过期");
                }
                if (userCoupon.getOrderId() != null) {
                    OrderInfo lockedOrder = orderInfoMapper.selectById(userCoupon.getOrderId());
                    if (lockedOrder != null && !OrderConstant.STATUS_CANCELED.equals(lockedOrder.getStatus())) {
                        throw new BusinessException("优惠券已锁定，请先支付或取消原订单");
                    }
                    releaseCouponLock(userCoupon, userCoupon.getOrderId());
                }
                couponTemplate = couponTemplateMapper.selectById(userCoupon.getCouponTemplateId());
                if (couponTemplate == null || !CouponConstant.STATUS_ENABLED.equals(couponTemplate.getStatus())) {
                    throw new NotFoundException("优惠券不存在");
                }
                if (couponTemplate.getMerchantId() != null && !couponTemplate.getMerchantId().equals(orderCreateDTO.getMerchantId())) {
                    throw new NotFoundException("优惠券不适用于当前商户");
                }
                if (couponTemplate.getProductId() != null && !couponTemplate.getProductId().equals(orderCreateDTO.getProductId())) {
                    throw new BusinessException("优惠券不适用于当前商品");
                }
                if (couponTemplate.getThresholdAmount() != null && totalAmount.compareTo(couponTemplate.getThresholdAmount()) < 0) {
                    throw new BusinessException("不满足优惠券使用门槛");
                }
                discountAmount = couponTemplate.getDiscountValue() == null
                        ? BigDecimal.ZERO
                        : couponTemplate.getDiscountValue();
                if (discountAmount.compareTo(totalAmount) > 0) {
                    discountAmount = totalAmount;
                }
            }
            BigDecimal payAmount = totalAmount.subtract(discountAmount);
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderNo(generateOrderNo());
            orderInfo.setUserId(currentId);
            orderInfo.setMerchantId(orderCreateDTO.getMerchantId());
            orderInfo.setProductId(orderCreateDTO.getProductId());
            orderInfo.setCouponId(orderCreateDTO.getCouponId());
            orderInfo.setTotalAmount(totalAmount);
            orderInfo.setDiscountAmount(discountAmount);
            orderInfo.setPayAmount(payAmount);
            orderInfo.setStatus(OrderConstant.STATUS_PENDING_PAY);
            orderInfo.setRemark(orderCreateDTO.getRemark());
            orderInfoMapper.insert(orderInfo);
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderInfo.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductCover(product.getCoverUrl());
            orderItem.setSalePrice(product.getSalePrice());
            orderItem.setQuantity(orderCreateDTO.getQuantity());
            orderItem.setAmount(totalAmount);
            orderItemMapper.insert(orderItem);
            int stockRows = productMapper.deductStockIfEnough(
                    product.getId(),
                    orderCreateDTO.getQuantity(),
                    ProductConstant.STATUS_ON_SHELF
            );
            if (stockRows <= 0) {
                throw new BusinessException("商品库存不足");
            }
            productService.evictProductReadCaches(product.getId());
            if (userCoupon != null) {
                lockCouponForOrder(userCoupon.getId(), currentId, orderInfo.getId());
            }
            deleteOrderPageCache(currentId);
            sendOrderTimeoutMessageAfterCommit(orderInfo.getId());
            orderCreated = true;
            return orderInfo.getId();
        } finally {
            if (!orderCreated && orderSubmitKey != null) {
                deleteOrderSubmitLock(orderSubmitKey);
            }
        }
    }

    private void validateSeckillProduct(Product product, Long merchantId) {
        if (product == null || !ProductConstant.STATUS_ON_SHELF.equals(product.getStatus())) {
            throw new NotFoundException("商品不存在");
        }
        if (!product.getMerchantId().equals(merchantId)) {
            throw new ParameterException("商户信息不正确");
        }
        LocalDateTime now = LocalDateTime.now();
        if (product.getStartTime() != null && product.getStartTime().isAfter(now)) {
            throw new BusinessException(SeckillService.FAILURE_CODE_ACTIVITY_NOT_STARTED, "秒杀活动未开始");
        }
        if (product.getEndTime() != null && !product.getEndTime().isAfter(now)) {
            throw new BusinessException(SeckillService.FAILURE_CODE_ACTIVITY_ENDED, "秒杀活动已结束");
        }
    }

    private AsyncOrderCreateDTO buildAsyncOrderCreateDTO(Long userId,
                                                         SeckillOrderCreateDTO seckillOrderCreateDTO,
                                                         String requestId) {
        AsyncOrderCreateDTO asyncOrderCreateDTO = new AsyncOrderCreateDTO();
        asyncOrderCreateDTO.setMessageId(requestId);
        asyncOrderCreateDTO.setUserId(userId);
        asyncOrderCreateDTO.setMerchantId(seckillOrderCreateDTO.getMerchantId());
        asyncOrderCreateDTO.setProductId(seckillOrderCreateDTO.getProductId());
        asyncOrderCreateDTO.setRemark(seckillOrderCreateDTO.getRemark());
        return asyncOrderCreateDTO;
    }

    private OrderCreateDTO buildSeckillOrderCreateOrderDTO(AsyncOrderCreateDTO asyncOrderCreateDTO) {
        OrderCreateDTO orderCreateDTO = new OrderCreateDTO();
        orderCreateDTO.setMerchantId(asyncOrderCreateDTO.getMerchantId());
        orderCreateDTO.setProductId(asyncOrderCreateDTO.getProductId());
        orderCreateDTO.setQuantity(1);
        orderCreateDTO.setRemark(asyncOrderCreateDTO.getRemark());
        return orderCreateDTO;
    }

    private void validateAsyncOrderCreateDTO(AsyncOrderCreateDTO asyncOrderCreateDTO) {
        if (asyncOrderCreateDTO == null
                || !StringUtils.hasText(asyncOrderCreateDTO.getMessageId())
                || asyncOrderCreateDTO.getUserId() == null
                || asyncOrderCreateDTO.getMerchantId() == null
                || asyncOrderCreateDTO.getProductId() == null) {
            throw new ParameterException("异步下单消息不完整");
        }
    }

    private boolean hasConsumedAsyncOrderMessage(String messageId) {
        LambdaQueryWrapper<MqConsumeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MqConsumeLog::getMsgId, messageId)
                .eq(MqConsumeLog::getConsumeStatus, MqConstant.CONSUME_STATUS_SUCCESS)
                .last("limit 1");
        return mqConsumeLogMapper.selectCount(queryWrapper) > 0;
    }

    private void saveAsyncOrderConsumeLog(AsyncOrderCreateDTO asyncOrderCreateDTO, Long orderId) {
        MqConsumeLog mqConsumeLog = new MqConsumeLog();
        LocalDateTime now = LocalDateTime.now();
        mqConsumeLog.setId(IdWorker.getId());
        mqConsumeLog.setMsgId(asyncOrderCreateDTO.getMessageId());
        mqConsumeLog.setBizType(MqConstant.BIZ_TYPE_ASYNC_ORDER_CREATE);
        mqConsumeLog.setBizKey(asyncOrderCreateDTO.getUserId() + ":" + asyncOrderCreateDTO.getProductId());
        mqConsumeLog.setConsumeStatus(MqConstant.CONSUME_STATUS_SUCCESS);
        mqConsumeLog.setConsumeTime(now);
        mqConsumeLog.setRemark("orderId=" + orderId);
        mqConsumeLog.setDeleted(0);
        mqConsumeLog.setCreateTime(now);
        mqConsumeLog.setUpdateTime(now);
        mqConsumeLogMapper.insert(mqConsumeLog);
    }

    private void registerSeckillOrderSuccessAfterCommit(Long orderId, Long userId, Long productId) {
        if (orderId == null || userId == null || productId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            seckillService.markOrderCreated(orderId, userId, productId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                seckillService.markOrderCreated(orderId, userId, productId);
            }
        });
    }

    private int resolveSeckillFailureCode(Exception e) {
        if (e instanceof BaseException baseException && isSeckillFailureCode(baseException.getCode())) {
            return baseException.getCode();
        }
        if (e instanceof BusinessException && e.getMessage() != null) {
            if (e.getMessage().contains("库存不足") || e.getMessage().contains("已抢完")) {
                return SeckillService.FAILURE_CODE_SOLD_OUT;
            }
            if (e.getMessage().contains("重复")) {
                return SeckillService.FAILURE_CODE_REPEAT;
            }
            if (e.getMessage().contains("未开始")) {
                return SeckillService.FAILURE_CODE_ACTIVITY_NOT_STARTED;
            }
            if (e.getMessage().contains("已结束")) {
                return SeckillService.FAILURE_CODE_ACTIVITY_ENDED;
            }
        }
        return SeckillService.FAILURE_CODE_SYSTEM_BUSY;
    }

    private boolean isSeckillFailureCode(Integer code) {
        return SeckillService.FAILURE_CODE_REPEAT == code
                || SeckillService.FAILURE_CODE_SOLD_OUT == code
                || SeckillService.FAILURE_CODE_ACTIVITY_NOT_STARTED == code
                || SeckillService.FAILURE_CODE_ACTIVITY_ENDED == code
                || SeckillService.FAILURE_CODE_SYSTEM_BUSY == code;
    }

    private String resolveSeckillFailureMessage(Exception e) {
        if (e == null || !StringUtils.hasText(e.getMessage())) {
            return "抢购失败，请稍后重试";
        }
        return e.getMessage();
    }

    private OrderDetailVO buildOrderDetailVO(OrderInfo orderInfo) {
        OrderDetailVO orderDetailVO = new OrderDetailVO();
        orderDetailVO.setId(orderInfo.getId());
        orderDetailVO.setOrderNo(orderInfo.getOrderNo());
        orderDetailVO.setUserId(orderInfo.getUserId());
        orderDetailVO.setMerchantId(orderInfo.getMerchantId());
        orderDetailVO.setProductId(orderInfo.getProductId());
        orderDetailVO.setCouponId(orderInfo.getCouponId());
        orderDetailVO.setTotalAmount(orderInfo.getTotalAmount());
        orderDetailVO.setDiscountAmount(orderInfo.getDiscountAmount());
        orderDetailVO.setPayAmount(orderInfo.getPayAmount());
        orderDetailVO.setStatus(orderInfo.getStatus());
        orderDetailVO.setRemark(orderInfo.getRemark());
        orderDetailVO.setPayTime(orderInfo.getPayTime());
        orderDetailVO.setCancelTime(orderInfo.getCancelTime());
        orderDetailVO.setFinishTime(orderInfo.getFinishTime());
        orderDetailVO.setCreateTime(orderInfo.getCreateTime());

        SysUser user = sysUserMapper.selectById(orderInfo.getUserId());
        if (user != null) {
            orderDetailVO.setUserNickname(user.getNickname());
        }

        Merchant merchant = merchantMapper.selectById(orderInfo.getMerchantId());
        if (merchant != null) {
            orderDetailVO.setMerchantName(merchant.getName());
        }

        OrderItem orderItem = getOrderItem(orderInfo.getId());
        if (orderItem != null) {
            orderDetailVO.setProductName(orderItem.getProductName());
        }

        return orderDetailVO;
    }

    private OrderInfo getUserOrder(Long id, Long userId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(id);
        if (orderInfo == null || !userId.equals(orderInfo.getUserId())) {
            throw new NotFoundException("订单不存在");
        }
        return orderInfo;
    }

    private OrderItem getOrderItem(Long orderId) {
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectOne(queryWrapper);
    }

    private void lockCouponForOrder(Long couponId, Long userId, Long orderId) {
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserCoupon::getOrderId, orderId)
                .set(UserCoupon::getUseTime, null)
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_UNUSED)
                .isNull(UserCoupon::getOrderId);
        int rows = userCouponMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new BusinessException("优惠券已锁定，请刷新后重试");
        }
    }

    private void releaseCouponLock(UserCoupon userCoupon, Long orderId) {
        Integer targetStatus = CouponConstant.USER_COUPON_UNUSED;
        if (userCoupon.getExpireTime() != null && !userCoupon.getExpireTime().isAfter(LocalDateTime.now())) {
            targetStatus = CouponConstant.USER_COUPON_EXPIRED;
        }

        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserCoupon::getStatus, targetStatus)
                .set(UserCoupon::getOrderId, null)
                .set(UserCoupon::getUseTime, null)
                .eq(UserCoupon::getId, userCoupon.getId())
                .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_UNUSED)
                .eq(UserCoupon::getOrderId, orderId);
        userCouponMapper.update(null, updateWrapper);
    }

    private void cancelPendingOrder(Long orderId, LocalDateTime cancelTime) {
        LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OrderInfo::getStatus, OrderConstant.STATUS_CANCELED)
                .set(OrderInfo::getCancelTime, cancelTime)
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus, OrderConstant.STATUS_PENDING_PAY);
        int rows = orderInfoMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
    }

    private void refreshPendingPayment(Long paymentRecordId, String payChannel) {
        LambdaUpdateWrapper<PaymentRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(PaymentRecord::getPayChannel, payChannel)
                .set(PaymentRecord::getTransactionNo, generateTransactionNo())
                .eq(PaymentRecord::getId, paymentRecordId)
                .eq(PaymentRecord::getPayStatus, PaymentConstant.STATUS_PENDING);
        int rows = paymentRecordMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new BusinessException("支付记录状态已变化，请稍后重试");
        }
    }

    private String generateOrderNo() {
        return "HX" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private boolean tryAcquireOrderSubmitLock(String key) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    "1",
                    CacheConstant.ORDER_SUBMIT_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("获取下单防重复提交锁失败, key={}", key, e);
            return true;
        }
    }

    private void deleteOrderSubmitLock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除下单防重复提交锁失败, key={}", key, e);
        }
    }

    private String buildOrderSubmitKey(Long userId, OrderCreateDTO orderCreateDTO) {
        String couponId = orderCreateDTO.getCouponId() == null ? "0" : orderCreateDTO.getCouponId().toString();
        return CacheConstant.ORDER_SUBMIT_KEY_PREFIX
                + userId
                + ":"
                + orderCreateDTO.getMerchantId()
                + ":"
                + orderCreateDTO.getProductId()
                + ":"
                + couponId
                + ":"
                + orderCreateDTO.getQuantity();
    }

    private boolean tryAcquireOrderPaySubmitLock(String key) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    "1",
                    CacheConstant.ORDER_PAY_SUBMIT_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("获取支付提交防重复锁失败, key={}", key, e);
            return true;
        }
    }

    private void deleteOrderPaySubmitLock(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除支付提交防重复锁失败, key={}", key, e);
        }
    }

    private String buildOrderPaySubmitKey(Long userId, PaymentCreateDTO paymentCreateDTO) {
        return CacheConstant.ORDER_PAY_SUBMIT_KEY_PREFIX
                + userId
                + ":"
                + paymentCreateDTO.getOrderId()
                + ":"
                + paymentCreateDTO.getPayChannel();
    }

    private void sendOrderTimeoutMessageAfterCommit(Long orderId) {
        if (orderId == null) {
            return;
        }
        MqNotifyDTO mqNotifyDTO = buildOrderTimeoutMqNotifyDTO(orderId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mqMessageService.sendOrderTimeoutMessage(mqNotifyDTO, MqConstant.ORDER_TIMEOUT_DELAY_MILLIS);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mqMessageService.sendOrderTimeoutMessage(mqNotifyDTO, MqConstant.ORDER_TIMEOUT_DELAY_MILLIS);
            }
        });
    }

    private MqNotifyDTO buildOrderTimeoutMqNotifyDTO(Long orderId) {
        MqNotifyDTO mqNotifyDTO = new MqNotifyDTO();
        mqNotifyDTO.setMessageId(IdWorker.getIdStr());
        mqNotifyDTO.setBizType(MqConstant.BIZ_TYPE_ORDER_TIMEOUT);
        mqNotifyDTO.setBizId(String.valueOf(orderId));
        return mqNotifyDTO;
    }

    private String generateTransactionNo() {
        return "PAY" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private String generateRefundNo() {
        return "RF" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    @SuppressWarnings("unchecked")
    private Page<OrderDetailVO> getOrderPageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<OrderDetailVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取用户端订单分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setOrderPageCache(String key, Page<OrderDetailVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入用户端订单分页缓存失败, key={}", key, e);
        }
    }

    private OrderDetailVO getOrderDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof OrderDetailVO orderDetailVO) {
                return orderDetailVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取用户端订单详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private OrderDetailVO getAdminOrderDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof OrderDetailVO orderDetailVO) {
                return orderDetailVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取管理端订单详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setOrderDetailCache(String key, OrderDetailVO orderDetailVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, orderDetailVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入用户端订单详情缓存失败, key={}", key, e);
        }
    }

    private void setAdminOrderDetailCache(String key, OrderDetailVO orderDetailVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, orderDetailVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入管理端订单详情缓存失败, key={}", key, e);
        }
    }

    private void deleteOrderDetailCache(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return;
        }
        String key = buildOrderDetailCacheKey(userId, orderId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除用户端订单详情缓存失败, key={}", key, e);
        }
    }

    private void deleteAdminOrderDetailCache(Long orderId) {
        if (orderId == null) {
            return;
        }
        String key = buildAdminOrderDetailCacheKey(orderId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除管理端订单详情缓存失败, key={}", key, e);
        }
    }

    private void deleteOrderPageCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(buildOrderPageCachePattern(userId));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端订单分页缓存失败, userId={}", userId, e);
        }
    }

    private String buildOrderDetailCacheKey(Long userId, Long orderId) {
        return CacheConstant.ORDER_DETAIL_KEY_PREFIX + userId + ":" + orderId;
    }

    private String buildAdminOrderDetailCacheKey(Long orderId) {
        return CacheConstant.ADMIN_ORDER_DETAIL_KEY_PREFIX + orderId;
    }

    private boolean shouldUseOrderPageCache(OrderQuery orderQuery) {
        return orderQuery != null
                && Integer.valueOf(1).equals(orderQuery.getPageNo())
                && orderQuery.getUserId() == null
                && !StringUtils.hasText(orderQuery.getOrderNo())
                && orderQuery.getMerchantId() == null
                && orderQuery.getProductId() == null
                && orderQuery.getBeginCreateTime() == null
                && orderQuery.getEndCreateTime() == null
                && orderQuery.getBeginPayTime() == null
                && orderQuery.getEndPayTime() == null;
    }

    private String buildOrderPageCacheKey(Long userId, OrderQuery orderQuery) {
        String status = orderQuery.getStatus() == null ? "all" : String.valueOf(orderQuery.getStatus());
        return CacheConstant.ORDER_PAGE_KEY_PREFIX
                + "user:" + userId
                + ":size:" + orderQuery.getPageSize()
                + ":status:" + status;
    }

    private String buildOrderPageCachePattern(Long userId) {
        return CacheConstant.ORDER_PAGE_KEY_PREFIX
                + "user:" + userId
                + ":*";
    }
}
