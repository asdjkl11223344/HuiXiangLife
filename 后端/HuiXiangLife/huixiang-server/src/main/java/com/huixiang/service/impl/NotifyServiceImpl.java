package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.CouponConstant;
import com.huixiang.constant.MqConstant;
import com.huixiang.constant.OrderConstant;
import com.huixiang.constant.PaymentConstant;
import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.dto.PayNotifyDTO;
import com.huixiang.entity.*;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.mapper.*;
import com.huixiang.service.MqMessageService;
import com.huixiang.service.NotifyService;
import com.huixiang.service.ProductService;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.NotifyAckVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserCouponMapper userCouponMapper;
    private final ProductService productService;
    private final MqMessageService mqMessageService;
    private final SeckillService seckillService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public Boolean payCallback(PayNotifyDTO payNotifyDTO) {
        validatePayStatus(payNotifyDTO.getPayStatus());
        LambdaQueryWrapper<PaymentRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentRecord::getTransactionNo, payNotifyDTO.getTransactionNo());
        PaymentRecord paymentRecord = paymentRecordMapper.selectOne(queryWrapper);
        if (paymentRecord==null){
            throw new NotFoundException("支付记录不存在");
        }
        OrderInfo orderInfo = orderInfoMapper.selectById(paymentRecord.getOrderId());
        if (orderInfo==null){
            throw new NotFoundException("订单不存在");
        }
        if (StringUtils.hasText(payNotifyDTO.getPayChannel())&&!payNotifyDTO.getPayChannel().equals(paymentRecord.getPayChannel())){
            throw new ParameterException("支付渠道不匹配");
        }
        if (StringUtils.hasText(orderInfo.getOrderNo())&&!payNotifyDTO.getOrderNo().equals(orderInfo.getOrderNo())){
            throw new ParameterException("订单编号不匹配");
        }
        if (PaymentConstant.STATUS_SUCCESS.equals(payNotifyDTO.getPayStatus())){
            return handleSuccessCallback(paymentRecord, orderInfo, payNotifyDTO);
        }
        return handleFailedCallback(paymentRecord, payNotifyDTO);
    }

    @Override
    @Transactional
    public NotifyAckVO handleOrderTimeout(MqNotifyDTO mqNotifyDTO) {
        validateMqNotifyDTO(mqNotifyDTO);
        Long orderId = parseBizId(mqNotifyDTO.getBizId());
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo==null){
            throw new NotFoundException("订单不存在");
        }
        if (!OrderConstant.STATUS_PENDING_PAY.equals(orderInfo.getStatus())) {
            return buildAck("订单无需关闭");
        }
        LocalDateTime cancelTime = LocalDateTime.now();
        if (!cancelPendingOrder(orderInfo.getId(), cancelTime)) {
            return buildAck("订单无需关闭");
        }
        orderInfo.setStatus(OrderConstant.STATUS_CANCELED);
        orderInfo.setCancelTime(cancelTime);
        OrderItem orderItem = getOrderItem(orderInfo.getId());
        if (orderItem != null) {
            int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int rows = productMapper.increaseStock(orderItem.getProductId(), quantity);
            if (rows > 0) {
                productService.evictProductReadCaches(orderItem.getProductId());
                seckillService.restoreStockForCanceledOrder(orderInfo.getId(), orderItem.getProductId());
            }
        }
        if (orderInfo.getCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(orderInfo.getCouponId());
            if (userCoupon != null) {
                releaseCouponLock(userCoupon, orderInfo.getId());
            }
        }
        deleteOrderDetailCache(orderInfo.getUserId(), orderInfo.getId());
        deleteAdminOrderDetailCache(orderInfo.getId());
        deleteOrderPageCache(orderInfo.getUserId());
        return buildAck("订单超时处理成功");
    }

    @Override
    public NotifyAckVO handleCouponExpire(MqNotifyDTO mqNotifyDTO) {
        validateMqNotifyDTO(mqNotifyDTO);
        Long userCouponId = parseBizId(mqNotifyDTO.getBizId());
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new NotFoundException("用户优惠券不存在");
        }
        if (!CouponConstant.USER_COUPON_UNUSED.equals(userCoupon.getStatus())) {
            return buildAck("优惠券无需过期处理");
        }
        if (userCoupon.getExpireTime() == null) {
            return buildAck("优惠券未设置过期时间，无需处理");
        }
        if (userCoupon.getOrderId() != null) {
            return buildAck("优惠券已锁定，等待订单释放");
        }
        if (userCoupon.getExpireTime().isAfter(LocalDateTime.now())) {
            return buildAck("优惠券尚未到期");
        }
        userCoupon.setStatus(CouponConstant.USER_COUPON_EXPIRED);
        userCouponMapper.updateById(userCoupon);
        return buildAck("优惠券过期处理成功");
    }

    @Override
    public NotifyAckVO handleOrderStatusSync(MqNotifyDTO mqNotifyDTO) {
        validateMqNotifyDTO(mqNotifyDTO);
        Long orderId = parseBizId(mqNotifyDTO.getBizId());
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo == null) {
            throw new NotFoundException("订单不存在");
        }
        if (OrderConstant.STATUS_FINISHED.equals(orderInfo.getStatus())
                || OrderConstant.STATUS_CANCELED.equals(orderInfo.getStatus())
                || OrderConstant.STATUS_REFUNDED.equals(orderInfo.getStatus())) {
            return buildAck("订单状态无需同步");
        }

        Integer targetStatus = parseTargetStatus(mqNotifyDTO);
        if (targetStatus == null) {
            return buildAck("未指定目标状态，无需同步");
        }

        if (OrderConstant.STATUS_FINISHED.equals(targetStatus)) {
            if (!OrderConstant.STATUS_PAID.equals(orderInfo.getStatus())) {
                return buildAck("当前订单状态不可同步为已完成");
            }
            orderInfo.setStatus(OrderConstant.STATUS_FINISHED);
            orderInfo.setFinishTime(LocalDateTime.now());
            orderInfoMapper.updateById(orderInfo);
            deleteOrderDetailCache(orderInfo.getUserId(), orderInfo.getId());
            deleteAdminOrderDetailCache(orderInfo.getId());
            deleteOrderPageCache(orderInfo.getUserId());
            return buildAck("订单状态同步成功");
        }

        return buildAck("当前目标状态无需处理");
    }

    private Boolean handleSuccessCallback(PaymentRecord paymentRecord, OrderInfo orderInfo, PayNotifyDTO payNotifyDTO) {
        if (PaymentConstant.STATUS_SUCCESS.equals(paymentRecord.getPayStatus())
                || OrderConstant.STATUS_PAID.equals(orderInfo.getStatus())
                || OrderConstant.STATUS_FINISHED.equals(orderInfo.getStatus())) {
            return true;
        }
        if (!OrderConstant.STATUS_PENDING_PAY.equals(orderInfo.getStatus())) {
            throw new BusinessException("当前订单状态不可更新为已支付");
        }
        LocalDateTime payTime = payNotifyDTO.getPayTime() == null
                ? LocalDateTime.now()
                : payNotifyDTO.getPayTime();
        if (!payPendingOrder(orderInfo.getId(), payTime)) {
            OrderInfo latestOrder = orderInfoMapper.selectById(orderInfo.getId());
            if (latestOrder != null && (OrderConstant.STATUS_PAID.equals(latestOrder.getStatus())
                    || OrderConstant.STATUS_FINISHED.equals(latestOrder.getStatus()))) {
                return true;
            }
            throw new BusinessException("当前订单状态不可更新为已支付");
        }
        orderInfo.setStatus(OrderConstant.STATUS_PAID);
        orderInfo.setPayTime(payTime);
        if (!markPaymentSuccess(paymentRecord.getId(), payTime, payNotifyDTO.getCallbackContent())) {
            PaymentRecord latestRecord = paymentRecordMapper.selectById(paymentRecord.getId());
            if (latestRecord != null && PaymentConstant.STATUS_SUCCESS.equals(latestRecord.getPayStatus())) {
                return true;
            }
            throw new BusinessException("支付记录状态异常，请稍后重试");
        }
        if (orderInfo.getCouponId() != null) {
            useCoupon(orderInfo.getCouponId(), orderInfo.getId(), payTime);
        }
        OrderItem orderItem = getOrderItem(orderInfo.getId());
        if (orderItem != null) {
            int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int rows = productMapper.increaseSoldCount(orderItem.getProductId(), quantity);
            if (rows > 0) {
                productService.evictProductReadCaches(orderItem.getProductId());
            }
        }
        deleteOrderDetailCache(orderInfo.getUserId(), orderInfo.getId());
        deleteAdminOrderDetailCache(orderInfo.getId());
        deleteOrderPageCache(orderInfo.getUserId());
        sendOrderFinishedSyncMessageAfterCommit(orderInfo.getId());
        return true;
    }

    private Boolean handleFailedCallback(PaymentRecord paymentRecord, PayNotifyDTO payNotifyDTO) {
        if (PaymentConstant.STATUS_SUCCESS.equals(paymentRecord.getPayStatus())) {
            return true;
        }
        if (!markPaymentFailed(paymentRecord.getId(), payNotifyDTO.getCallbackContent())) {
            PaymentRecord latestRecord = paymentRecordMapper.selectById(paymentRecord.getId());
            if (latestRecord != null && !PaymentConstant.STATUS_PENDING.equals(latestRecord.getPayStatus())) {
                return true;
            }
        }
        return true;
    }

    private OrderItem getOrderItem(Long orderId) {
        LambdaQueryWrapper<OrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectOne(queryWrapper);
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

    private void useCoupon(Long couponId, Long orderId, LocalDateTime payTime) {
        LambdaUpdateWrapper<UserCoupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(UserCoupon::getStatus, CouponConstant.USER_COUPON_USED)
                .set(UserCoupon::getOrderId, orderId)
                .set(UserCoupon::getUseTime, payTime)
                .eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getStatus, CouponConstant.USER_COUPON_UNUSED)
                .eq(UserCoupon::getOrderId, orderId);
        int rows = userCouponMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new BusinessException("优惠券状态异常，请稍后重试");
        }
    }

    private boolean cancelPendingOrder(Long orderId, LocalDateTime cancelTime) {
        LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OrderInfo::getStatus, OrderConstant.STATUS_CANCELED)
                .set(OrderInfo::getCancelTime, cancelTime)
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus, OrderConstant.STATUS_PENDING_PAY);
        return orderInfoMapper.update(null, updateWrapper) > 0;
    }

    private boolean payPendingOrder(Long orderId, LocalDateTime payTime) {
        LambdaUpdateWrapper<OrderInfo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(OrderInfo::getStatus, OrderConstant.STATUS_PAID)
                .set(OrderInfo::getPayTime, payTime)
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus, OrderConstant.STATUS_PENDING_PAY);
        return orderInfoMapper.update(null, updateWrapper) > 0;
    }

    private boolean markPaymentSuccess(Long paymentRecordId, LocalDateTime payTime, String callbackContent) {
        LambdaUpdateWrapper<PaymentRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(PaymentRecord::getPayStatus, PaymentConstant.STATUS_SUCCESS)
                .set(PaymentRecord::getPayTime, payTime)
                .set(PaymentRecord::getCallbackContent, callbackContent)
                .eq(PaymentRecord::getId, paymentRecordId)
                .eq(PaymentRecord::getPayStatus, PaymentConstant.STATUS_PENDING);
        return paymentRecordMapper.update(null, updateWrapper) > 0;
    }

    private boolean markPaymentFailed(Long paymentRecordId, String callbackContent) {
        LambdaUpdateWrapper<PaymentRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(PaymentRecord::getPayStatus, PaymentConstant.STATUS_FAILED)
                .set(PaymentRecord::getCallbackContent, callbackContent)
                .eq(PaymentRecord::getId, paymentRecordId)
                .eq(PaymentRecord::getPayStatus, PaymentConstant.STATUS_PENDING);
        return paymentRecordMapper.update(null, updateWrapper) > 0;
    }

    private void validatePayStatus(Integer payStatus) {
        if (!PaymentConstant.STATUS_SUCCESS.equals(payStatus)
                && !PaymentConstant.STATUS_FAILED.equals(payStatus)) {
            throw new ParameterException("支付状态不正确");
        }
    }

    private void validateMqNotifyDTO(MqNotifyDTO mqNotifyDTO) {
        if (!StringUtils.hasText(mqNotifyDTO.getMessageId())
                || !StringUtils.hasText(mqNotifyDTO.getBizType())
                || !StringUtils.hasText(mqNotifyDTO.getBizId())) {
            throw new ParameterException("MQ通知参数不完整");
        }
    }

    private Long parseBizId(String bizId) {
        try {
            return Long.valueOf(bizId);
        } catch (NumberFormatException e) {
            throw new ParameterException("业务ID格式不正确");
        }
    }

    private Integer parseTargetStatus(MqNotifyDTO mqNotifyDTO) {
        if (mqNotifyDTO.getPayload() == null) {
            return null;
        }
        Object targetStatus = mqNotifyDTO.getPayload().get("targetStatus");
        if (targetStatus == null) {
            return null;
        }
        if (targetStatus instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(targetStatus));
        } catch (NumberFormatException e) {
            throw new ParameterException("目标状态格式不正确");
        }
    }

    private NotifyAckVO buildAck(String message) {
        NotifyAckVO notifyAckVO = new NotifyAckVO();
        notifyAckVO.setSuccess(true);
        notifyAckVO.setMessage(message);
        return notifyAckVO;
    }

    private void sendOrderFinishedSyncMessageAfterCommit(Long orderId) {
        if (orderId == null) {
            return;
        }
        MqNotifyDTO mqNotifyDTO = buildOrderFinishedSyncMqNotifyDTO(orderId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mqMessageService.sendOrderStatusSyncMessage(mqNotifyDTO, MqConstant.ORDER_STATUS_SYNC_DELAY_MILLIS);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mqMessageService.sendOrderStatusSyncMessage(mqNotifyDTO, MqConstant.ORDER_STATUS_SYNC_DELAY_MILLIS);
            }
        });
    }

    private MqNotifyDTO buildOrderFinishedSyncMqNotifyDTO(Long orderId) {
        MqNotifyDTO mqNotifyDTO = new MqNotifyDTO();
        mqNotifyDTO.setMessageId("order-status-sync-" + orderId);
        mqNotifyDTO.setBizType(MqConstant.BIZ_TYPE_ORDER_STATUS_SYNC);
        mqNotifyDTO.setBizId(String.valueOf(orderId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetStatus", OrderConstant.STATUS_FINISHED);
        mqNotifyDTO.setPayload(payload);
        return mqNotifyDTO;
    }

    private void deleteOrderDetailCache(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return;
        }
        String key = CacheConstant.ORDER_DETAIL_KEY_PREFIX + userId + ":" + orderId;
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
        String key = CacheConstant.ADMIN_ORDER_DETAIL_KEY_PREFIX + orderId;
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
            Set<String> keys = redisTemplate.keys(CacheConstant.ORDER_PAGE_KEY_PREFIX + "user:" + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端订单分页缓存失败, userId={}", userId, e);
        }
    }
}
