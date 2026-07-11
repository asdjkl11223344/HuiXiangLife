package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.CouponConstant;
import com.huixiang.constant.MqConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.CouponReceiveDTO;
import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.entity.CouponTemplate;
import com.huixiang.entity.UserCoupon;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.mapper.CouponTemplateMapper;
import com.huixiang.mapper.UserCouponMapper;
import com.huixiang.query.CouponQuery;
import com.huixiang.query.UserCouponQuery;
import com.huixiang.service.CouponService;
import com.huixiang.service.MqMessageService;
import com.huixiang.vo.CouponVO;
import com.huixiang.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final MqMessageService mqMessageService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<CouponVO> page(CouponQuery couponQuery) {
        String couponPageCacheKey = null;
        if (shouldUseCouponPageCache(couponQuery)) {
            couponPageCacheKey = buildCouponPageCacheKey(couponQuery);
            Page<CouponVO> cachePage = getCouponPageCache(couponPageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<CouponTemplate> page = new Page<>(couponQuery.getPageNo(), couponQuery.getPageSize());
        LambdaQueryWrapper<CouponTemplate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CouponTemplate::getStatus, CouponConstant.STATUS_ENABLED);
        if (couponQuery.getMerchantId() != null) {
            queryWrapper.eq(CouponTemplate::getMerchantId, couponQuery.getMerchantId());
        }
        if (couponQuery.getProductId() != null) {
            queryWrapper.eq(CouponTemplate::getProductId, couponQuery.getProductId());
        }
        if (couponQuery.getType() != null) {
            queryWrapper.eq(CouponTemplate::getType, couponQuery.getType());
        }
        Page<CouponTemplate> couponTemplatePage = couponTemplateMapper.selectPage(page, queryWrapper);
        List<CouponVO> records = couponTemplatePage.getRecords()
                .stream()
                .map(this::buildCouponVO)
                .toList();
        Page<CouponVO> resultPage = new Page<>(
                couponTemplatePage.getCurrent(),
                couponTemplatePage.getSize()
        );
        resultPage.setTotal(couponTemplatePage.getTotal());
        resultPage.setRecords(records);
        if (couponPageCacheKey != null) {
            long ttl = CacheConstant.USER_COUPON_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.USER_COUPON_PAGE_TTL_RANDOM_BOUND_MINUTES + 1);
            setCouponPageCache(couponPageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    @Transactional
    public Long receive(CouponReceiveDTO couponReceiveDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(couponReceiveDTO.getCouponTemplateId());
        if (couponTemplate==null||!CouponConstant.STATUS_ENABLED.equals(couponTemplate.getStatus())){
            throw new NotFoundException("优惠券不存在");
        }
        if (couponTemplate.getStock()==null||couponTemplate.getStock()<=0){
            throw new NotFoundException("优惠券库存不足");
        }
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, currentId)
                .eq(UserCoupon::getCouponTemplateId, couponReceiveDTO.getCouponTemplateId());
        Long receivedCount = userCouponMapper.selectCount(queryWrapper);
        if (couponTemplate.getLimitPerUser()!=null&&receivedCount>=couponTemplate.getLimitPerUser()){
            throw new BusinessException("已达到领取上限");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(currentId);
        userCoupon.setCouponTemplateId(couponReceiveDTO.getCouponTemplateId());
        userCoupon.setStatus(CouponConstant.USER_COUPON_UNUSED);
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpireTime(couponTemplate.getEndTime());
        userCouponMapper.insert(userCoupon);
        couponTemplate.setStock(couponTemplate.getStock() - 1);
        couponTemplateMapper.updateById(couponTemplate);
        deleteCouponPageCache();
        deleteCouponDetailCache(couponTemplate.getId());
        sendCouponExpireMessageAfterCommit(userCoupon);
        return userCoupon.getId();
    }

    @Override
    public Page<UserCouponVO> myPage(UserCouponQuery userCouponQuery) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        Page<UserCoupon> page = new Page<>(userCouponQuery.getPageNo(), userCouponQuery.getPageSize());
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getUserId, currentId);
        if (userCouponQuery.getStatus() != null) {
            queryWrapper.eq(UserCoupon::getStatus, userCouponQuery.getStatus());
        }
        queryWrapper.orderByDesc(UserCoupon::getReceiveTime);
        Page<UserCoupon> userCouponPage = userCouponMapper.selectPage(page, queryWrapper);
        List<UserCouponVO> records = userCouponPage.getRecords()
                .stream()
                .map(this::buildUserCouponVO)
                .toList();
        Page<UserCouponVO> resultPage = new Page<>(
                userCouponPage.getCurrent(),
                userCouponPage.getSize()
        );
        resultPage.setTotal(userCouponPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    private CouponVO buildCouponVO(CouponTemplate couponTemplate) {
        CouponVO couponVO = new CouponVO();
        couponVO.setId(couponTemplate.getId());
        couponVO.setName(couponTemplate.getName());
        couponVO.setType(couponTemplate.getType());
        couponVO.setDiscountType(couponTemplate.getDiscountType());
        couponVO.setDiscountValue(couponTemplate.getDiscountValue());
        couponVO.setThresholdAmount(couponTemplate.getThresholdAmount());
        couponVO.setStock(couponTemplate.getStock());
        couponVO.setLimitPerUser(couponTemplate.getLimitPerUser());
        couponVO.setStatus(couponTemplate.getStatus());
        couponVO.setStartTime(couponTemplate.getStartTime());
        couponVO.setEndTime(couponTemplate.getEndTime());
        return couponVO;
    }

    private UserCouponVO buildUserCouponVO(UserCoupon userCoupon) {
        UserCouponVO userCouponVO = new UserCouponVO();
        userCouponVO.setId(userCoupon.getId());
        userCouponVO.setUserId(userCoupon.getUserId());
        userCouponVO.setCouponTemplateId(userCoupon.getCouponTemplateId());
        userCouponVO.setStatus(userCoupon.getStatus());
        userCouponVO.setReceiveTime(userCoupon.getReceiveTime());
        userCouponVO.setUseTime(userCoupon.getUseTime());
        userCouponVO.setExpireTime(userCoupon.getExpireTime());
        userCouponVO.setOrderId(userCoupon.getOrderId());

        CouponTemplate couponTemplate = couponTemplateMapper.selectById(userCoupon.getCouponTemplateId());
        if (couponTemplate != null) {
            userCouponVO.setCouponName(couponTemplate.getName());
        }

        return userCouponVO;
    }

    @SuppressWarnings("unchecked")
    private Page<CouponVO> getCouponPageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<CouponVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取用户端优惠券分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setCouponPageCache(String key, Page<CouponVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入用户端优惠券分页缓存失败, key={}", key, e);
        }
    }

    private void deleteCouponPageCache() {
        try {
            Set<String> keys = redisTemplate.keys(CacheConstant.USER_COUPON_PAGE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端优惠券分页缓存失败", e);
        }
    }

    private void deleteCouponDetailCache(Long id) {
        try {
            redisTemplate.delete(CacheConstant.COUPON_DETAIL_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("删除优惠券模板详情缓存失败, id={}", id, e);
        }
    }

    private boolean shouldUseCouponPageCache(CouponQuery couponQuery) {
        return couponQuery != null
                && Integer.valueOf(1).equals(couponQuery.getPageNo())
                && couponQuery.getPageSize() != null
                && couponQuery.getPageSize() > 0;
    }

    private String buildCouponPageCacheKey(CouponQuery couponQuery) {
        StringBuilder key = new StringBuilder(CacheConstant.USER_COUPON_PAGE_KEY_PREFIX)
                .append(couponQuery.getPageSize())
                .append(":merchant:").append(couponQuery.getMerchantId() == null ? 0 : couponQuery.getMerchantId())
                .append(":product:").append(couponQuery.getProductId() == null ? 0 : couponQuery.getProductId())
                .append(":type:").append(couponQuery.getType() == null ? 0 : couponQuery.getType());
        if (couponQuery.getStatus() != null) {
            key.append(":status:").append(couponQuery.getStatus());
        }
        return key.toString();
    }

    private void sendCouponExpireMessageAfterCommit(UserCoupon userCoupon) {
        if (userCoupon == null || userCoupon.getId() == null || userCoupon.getExpireTime() == null) {
            return;
        }
        long delayMillis = java.time.Duration.between(LocalDateTime.now(), userCoupon.getExpireTime()).toMillis();
        if (delayMillis < 0L) {
            delayMillis = 0L;
        }
        MqNotifyDTO mqNotifyDTO = buildCouponExpireMqNotifyDTO(userCoupon.getId());
        long finalDelayMillis = delayMillis;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mqMessageService.sendCouponExpireMessage(mqNotifyDTO, finalDelayMillis);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mqMessageService.sendCouponExpireMessage(mqNotifyDTO, finalDelayMillis);
            }
        });
    }

    private MqNotifyDTO buildCouponExpireMqNotifyDTO(Long userCouponId) {
        MqNotifyDTO mqNotifyDTO = new MqNotifyDTO();
        mqNotifyDTO.setMessageId("coupon-expire-" + userCouponId);
        mqNotifyDTO.setBizType(MqConstant.BIZ_TYPE_COUPON_EXPIRE);
        mqNotifyDTO.setBizId(String.valueOf(userCouponId));
        return mqNotifyDTO;
    }
}
