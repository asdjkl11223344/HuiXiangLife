package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.CouponConstant;
import com.huixiang.dto.CouponCreateDTO;
import com.huixiang.dto.CouponUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.entity.CouponTemplate;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.Product;
import com.huixiang.entity.UserCoupon;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.mapper.CouponTemplateMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.mapper.UserCouponMapper;
import com.huixiang.query.CouponQuery;
import com.huixiang.service.CouponTemplateService;
import com.huixiang.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTemplateServiceImpl implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<CouponVO> page(CouponQuery couponQuery) {
        Page<CouponTemplate> page = new Page<>(couponQuery.getPageNo(), couponQuery.getPageSize());
        LambdaQueryWrapper<CouponTemplate> queryWrapper = new LambdaQueryWrapper<>();
        if (couponQuery.getMerchantId() != null) {
            queryWrapper.eq(CouponTemplate::getMerchantId, couponQuery.getMerchantId());
        }
        if (couponQuery.getProductId() != null) {
            queryWrapper.eq(CouponTemplate::getProductId, couponQuery.getProductId());
        }
        if (couponQuery.getType() != null) {
            queryWrapper.eq(CouponTemplate::getType, couponQuery.getType());
        }
        if (couponQuery.getStatus() != null) {
            queryWrapper.eq(CouponTemplate::getStatus, couponQuery.getStatus());
        }
        queryWrapper.orderByDesc(CouponTemplate::getCreateTime);
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
        return resultPage;
    }

    @Override
    public CouponVO detail(Long id) {
        String key = CacheConstant.COUPON_DETAIL_KEY_PREFIX + id;
        CouponVO cacheCoupon = getCouponDetailCache(key);
        if (cacheCoupon != null) {
            return cacheCoupon;
        }
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(id);
        if (couponTemplate == null) {
            throw new NotFoundException("优惠券模板不存在");
        }
        CouponVO couponVO = buildCouponVO(couponTemplate);
        long ttl = CacheConstant.COUPON_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.COUPON_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setCouponDetailCache(key, couponVO, ttl);
        return couponVO;
    }

    @Override
    public Long create(CouponCreateDTO couponCreateDTO) {
        validateTime(couponCreateDTO.getStartTime(), couponCreateDTO.getEndTime());
        validateValue(couponCreateDTO.getDiscountValue(), couponCreateDTO.getThresholdAmount(), couponCreateDTO.getStock(), couponCreateDTO.getLimitPerUser());
        validateBizTarget(couponCreateDTO.getMerchantId(), couponCreateDTO.getProductId());
        CouponTemplate couponTemplate = new CouponTemplate();
        couponTemplate.setName(couponCreateDTO.getName());
        couponTemplate.setType(couponCreateDTO.getType());
        couponTemplate.setDiscountType(couponCreateDTO.getDiscountType());
        couponTemplate.setDiscountValue(couponCreateDTO.getDiscountValue());
        couponTemplate.setThresholdAmount(couponCreateDTO.getThresholdAmount());
        couponTemplate.setStock(couponCreateDTO.getStock());
        couponTemplate.setLimitPerUser(couponCreateDTO.getLimitPerUser());
        couponTemplate.setMerchantId(couponCreateDTO.getMerchantId());
        couponTemplate.setProductId(couponCreateDTO.getProductId());
        couponTemplate.setStartTime(couponCreateDTO.getStartTime());
        couponTemplate.setEndTime(couponCreateDTO.getEndTime());
        couponTemplate.setStatus(CouponConstant.STATUS_ENABLED);
        couponTemplateMapper.insert(couponTemplate);
        deleteUserCouponPageCache();
        return couponTemplate.getId();
    }

    @Override
    public Boolean update(CouponUpdateDTO couponUpdateDTO) {
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(couponUpdateDTO.getId());
        if (couponTemplate == null){
            throw new NotFoundException("优惠券模板不存在");
        }
        validateTime(couponUpdateDTO.getStartTime(), couponUpdateDTO.getEndTime());
        validateValue(couponUpdateDTO.getDiscountValue(), couponUpdateDTO.getThresholdAmount(), couponUpdateDTO.getStock(), couponUpdateDTO.getLimitPerUser());
        validateBizTarget(couponUpdateDTO.getMerchantId(), couponUpdateDTO.getProductId());
        validateTemplateUpdate(couponTemplate, couponUpdateDTO);
        couponTemplate.setName(couponUpdateDTO.getName());
        couponTemplate.setType(couponUpdateDTO.getType());
        couponTemplate.setDiscountType(couponUpdateDTO.getDiscountType());
        couponTemplate.setDiscountValue(couponUpdateDTO.getDiscountValue());
        couponTemplate.setThresholdAmount(couponUpdateDTO.getThresholdAmount());
        couponTemplate.setStock(couponUpdateDTO.getStock());
        couponTemplate.setLimitPerUser(couponUpdateDTO.getLimitPerUser());
        couponTemplate.setMerchantId(couponUpdateDTO.getMerchantId());
        couponTemplate.setProductId(couponUpdateDTO.getProductId());
        couponTemplate.setStartTime(couponUpdateDTO.getStartTime());
        couponTemplate.setEndTime(couponUpdateDTO.getEndTime());
        couponTemplateMapper.updateById(couponTemplate);
        deleteUserCouponPageCache();
        deleteCouponDetailCache(couponTemplate.getId());
        return true;
    }

    @Override
    public Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO) {
        if (!CouponConstant.STATUS_ENABLED.equals(statusUpdateDTO.getStatus())
                && !CouponConstant.STATUS_DISABLED.equals(statusUpdateDTO.getStatus())) {
            throw new ParameterException("优惠券模板状态不正确");
        }
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(id);
        if (couponTemplate == null){
            throw new NotFoundException("优惠券模板不存在");
        }
        couponTemplate.setStatus(statusUpdateDTO.getStatus());
        couponTemplateMapper.updateById(couponTemplate);
        deleteUserCouponPageCache();
        deleteCouponDetailCache(couponTemplate.getId());
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        CouponTemplate couponTemplate = couponTemplateMapper.selectById(id);
        if (couponTemplate == null){
            throw new NotFoundException("优惠券模板不存在");
        }
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getCouponTemplateId, id);
        Long count = userCouponMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该优惠券模板已被领取，不能删除，请改为停用");
        }
        couponTemplateMapper.deleteById(id);
        deleteUserCouponPageCache();
        deleteCouponDetailCache(id);
        return true;
    }

    private void validateTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new ParameterException("优惠券时间范围不正确");
        }
    }

    private void validateValue(BigDecimal discountValue, BigDecimal thresholdAmount, Integer stock, Integer limitPerUser) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ParameterException("优惠值必须大于0");
        }
        if (thresholdAmount == null || thresholdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ParameterException("门槛金额不能小于0");
        }
        if (stock == null || stock < 0) {
            throw new ParameterException("库存不能小于0");
        }
        if (limitPerUser == null || limitPerUser < 1) {
            throw new ParameterException("每人限领数量不能小于1");
        }
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

    private void validateBizTarget(Long merchantId, Long productId) {
        Merchant merchant = null;
        if (merchantId != null) {
            merchant = merchantMapper.selectById(merchantId);
            if (merchant == null) {
                throw new NotFoundException("商户不存在");
            }
        }

        if (productId != null) {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                throw new NotFoundException("商品不存在");
            }
            if (merchantId != null && !merchantId.equals(product.getMerchantId())) {
                throw new ParameterException("商品不属于当前商户");
            }
        }
    }

    private void validateTemplateUpdate(CouponTemplate couponTemplate, CouponUpdateDTO couponUpdateDTO) {
        LambdaQueryWrapper<UserCoupon> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserCoupon::getCouponTemplateId, couponTemplate.getId());
        Long count = userCouponMapper.selectCount(queryWrapper);
        if (count == null || count == 0) {
            return;
        }
        boolean coreChanged = !couponTemplate.getName().equals(couponUpdateDTO.getName())
                || !couponTemplate.getType().equals(couponUpdateDTO.getType())
                || !couponTemplate.getDiscountType().equals(couponUpdateDTO.getDiscountType())
                || couponTemplate.getDiscountValue().compareTo(couponUpdateDTO.getDiscountValue()) != 0
                || couponTemplate.getThresholdAmount().compareTo(couponUpdateDTO.getThresholdAmount()) != 0
                || !java.util.Objects.equals(couponTemplate.getMerchantId(), couponUpdateDTO.getMerchantId())
                || !java.util.Objects.equals(couponTemplate.getProductId(), couponUpdateDTO.getProductId());
        if (coreChanged) {
            throw new BusinessException("该优惠券模板已被领取，不允许修改核心信息");
        }
    }

    private void deleteUserCouponPageCache() {
        try {
            Set<String> keys = redisTemplate.keys(CacheConstant.USER_COUPON_PAGE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端优惠券分页缓存失败", e);
        }
    }

    private CouponVO getCouponDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof CouponVO couponVO) {
                return couponVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取优惠券模板详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setCouponDetailCache(String key, CouponVO couponVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, couponVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入优惠券模板详情缓存失败, key={}", key, e);
        }
    }

    private void deleteCouponDetailCache(Long id) {
        try {
            redisTemplate.delete(CacheConstant.COUPON_DETAIL_KEY_PREFIX + id);
        } catch (Exception e) {
            log.warn("删除优惠券模板详情缓存失败, id={}", id, e);
        }
    }
}
