package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.OrderConstant;
import com.huixiang.constant.ReviewConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.ReviewCreateDTO;
import com.huixiang.entity.OrderInfo;
import com.huixiang.entity.Review;
import com.huixiang.entity.SysUser;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.mapper.OrderInfoMapper;
import com.huixiang.mapper.ReviewMapper;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.query.ReviewQuery;
import com.huixiang.service.ReviewService;
import com.huixiang.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Long create(ReviewCreateDTO reviewCreateDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId==null){
            throw new UnauthorizedException("请先登录");
        }
        OrderInfo orderInfo = orderInfoMapper.selectById(reviewCreateDTO.getOrderId());
        if (orderInfo==null||!currentId.equals(orderInfo.getUserId())){
            throw new NotFoundException("订单不存在");
        }
        if (!reviewCreateDTO.getMerchantId().equals(orderInfo.getMerchantId())){
            throw new ParameterException("商户信息不正确");
        }
        if (!reviewCreateDTO.getProductId().equals(orderInfo.getProductId())){
            throw new ParameterException("商品信息不正确");
        }
        if (!OrderConstant.STATUS_PAID.equals(orderInfo.getStatus())
                && !OrderConstant.STATUS_FINISHED.equals(orderInfo.getStatus())) {
            throw new BusinessException("当前订单状态不可评价");
        }
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getOrderId, reviewCreateDTO.getOrderId());
        Long count = reviewMapper.selectCount(queryWrapper);
        if (count!=null&&count>0){
            throw new BusinessException("该订单已评价");
        }
        Review review = new Review();
        review.setOrderId(reviewCreateDTO.getOrderId());
        review.setUserId(currentId);
        review.setMerchantId(reviewCreateDTO.getMerchantId());
        review.setProductId(reviewCreateDTO.getProductId());
        review.setScore(reviewCreateDTO.getScore());
        review.setContent(reviewCreateDTO.getContent());
        review.setStatus(ReviewConstant.STATUS_VISIBLE);
        reviewMapper.insert(review);
        deleteReviewPageCache(review.getMerchantId(), review.getProductId());
        return review.getId();
    }

    @Override
    public Page<ReviewVO> page(ReviewQuery reviewQuery) {
        String reviewPageCacheKey = null;
        if (shouldUseReviewPageCache(reviewQuery)) {
            reviewPageCacheKey = buildReviewPageCacheKey(reviewQuery);
            Page<ReviewVO> cachePage = getReviewPageCache(reviewPageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<Review> page = new Page<>(reviewQuery.getPageNo(), reviewQuery.getPageSize());
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Review::getStatus, ReviewConstant.STATUS_VISIBLE);
        if (reviewQuery.getMerchantId() != null) {
            queryWrapper.eq(Review::getMerchantId, reviewQuery.getMerchantId());
        }
        if (reviewQuery.getProductId() != null) {
            queryWrapper.eq(Review::getProductId, reviewQuery.getProductId());
        }
        queryWrapper.orderByDesc(Review::getCreateTime);
        Page<Review> reviewPage = reviewMapper.selectPage(page, queryWrapper);
        List<ReviewVO> records = reviewPage.getRecords()
                .stream()
                .map(this::buildReviewVO)
                .toList();
        Page<ReviewVO> resultPage = new Page<>(
                reviewPage.getCurrent(),
                reviewPage.getSize()
        );
        resultPage.setTotal(reviewPage.getTotal());
        resultPage.setRecords(records);
        if (reviewPageCacheKey != null) {
            long ttl = CacheConstant.REVIEW_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.REVIEW_PAGE_TTL_RANDOM_BOUND_MINUTES + 1);
            setReviewPageCache(reviewPageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    public Page<ReviewVO> adminPage(ReviewQuery reviewQuery) {
        Page<Review> page = new Page<>(reviewQuery.getPageNo(), reviewQuery.getPageSize());
        LambdaQueryWrapper<Review> queryWrapper = new LambdaQueryWrapper<>();
        if (reviewQuery.getMerchantId() != null) {
            queryWrapper.eq(Review::getMerchantId, reviewQuery.getMerchantId());
        }

        if (reviewQuery.getProductId() != null) {
            queryWrapper.eq(Review::getProductId, reviewQuery.getProductId());
        }

        if (reviewQuery.getUserId() != null) {
            queryWrapper.eq(Review::getUserId, reviewQuery.getUserId());
        }

        if (reviewQuery.getStatus() != null) {
            queryWrapper.eq(Review::getStatus, reviewQuery.getStatus());
        }
        queryWrapper.orderByDesc(Review::getCreateTime);
        Page<Review> reviewPage = reviewMapper.selectPage(page, queryWrapper);
        List<ReviewVO> records = reviewPage.getRecords()
                .stream()
                .map(this::buildReviewVO)
                .toList();
        Page<ReviewVO> resultPage = new Page<>(
                reviewPage.getCurrent(),
                reviewPage.getSize()
        );
        resultPage.setTotal(reviewPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public ReviewVO adminDetail(Long id) {
        String key = CacheConstant.ADMIN_REVIEW_DETAIL_KEY_PREFIX + id;
        ReviewVO cacheReview = getAdminReviewDetailCache(key);
        if (cacheReview != null) {
            return cacheReview;
        }
        Review review = reviewMapper.selectById(id);
        if (review == null){
            throw new NotFoundException("评价不存在");
        }
        ReviewVO reviewVO = buildReviewVO(review);
        long ttl = CacheConstant.ADMIN_REVIEW_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.ADMIN_REVIEW_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setAdminReviewDetailCache(key, reviewVO, ttl);
        return reviewVO;
    }

    @Override
    public Boolean adminUpdateStatus(Long id, Integer status) {
        if (!ReviewConstant.STATUS_HIDDEN.equals(status) && !ReviewConstant.STATUS_VISIBLE.equals(status)) {
            throw new ParameterException("评价状态不正确");
        }
        Review review = reviewMapper.selectById(id);
        if (review == null){
            throw new NotFoundException("评价不存在");
        }
        review.setStatus(status);
        reviewMapper.updateById(review);
        deleteReviewPageCache(review.getMerchantId(), review.getProductId());
        deleteAdminReviewDetailCache(review.getId());
        return true;
    }

    private ReviewVO getAdminReviewDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof ReviewVO reviewVO) {
                return reviewVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取管理端评价详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setAdminReviewDetailCache(String key, ReviewVO reviewVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, reviewVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入管理端评价详情缓存失败, key={}", key, e);
        }
    }

    private void deleteAdminReviewDetailCache(Long reviewId) {
        if (reviewId == null) {
            return;
        }
        try {
            redisTemplate.delete(CacheConstant.ADMIN_REVIEW_DETAIL_KEY_PREFIX + reviewId);
        } catch (Exception e) {
            log.warn("删除管理端评价详情缓存失败, reviewId={}", reviewId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Page<ReviewVO> getReviewPageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<ReviewVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取用户端评价分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setReviewPageCache(String key, Page<ReviewVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入用户端评价分页缓存失败, key={}", key, e);
        }
    }

    private void deleteReviewPageCache(Long merchantId, Long productId) {
        try {
            Set<String> keys = redisTemplate.keys(buildReviewPageCachePattern(merchantId, productId));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端评价分页缓存失败, merchantId={}, productId={}", merchantId, productId, e);
        }
    }

    private boolean shouldUseReviewPageCache(ReviewQuery reviewQuery) {
        return reviewQuery != null
                && Integer.valueOf(1).equals(reviewQuery.getPageNo())
                && reviewQuery.getMerchantId() != null
                && reviewQuery.getProductId() != null
                && reviewQuery.getUserId() == null
                && reviewQuery.getStatus() == null;
    }

    private String buildReviewPageCacheKey(ReviewQuery reviewQuery) {
        return CacheConstant.REVIEW_PAGE_KEY_PREFIX
                + "size:" + reviewQuery.getPageSize()
                + ":merchant:" + reviewQuery.getMerchantId()
                + ":product:" + reviewQuery.getProductId();
    }

    private String buildReviewPageCachePattern(Long merchantId, Long productId) {
        return CacheConstant.REVIEW_PAGE_KEY_PREFIX
                + "*:merchant:" + merchantId
                + ":product:" + productId;
    }

    private ReviewVO buildReviewVO(Review review) {
        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setId(review.getId());
        reviewVO.setOrderId(review.getOrderId());
        reviewVO.setUserId(review.getUserId());
        reviewVO.setMerchantId(review.getMerchantId());
        reviewVO.setProductId(review.getProductId());
        reviewVO.setScore(review.getScore());
        reviewVO.setContent(review.getContent());
        reviewVO.setStatus(review.getStatus());
        reviewVO.setCreateTime(review.getCreateTime());

        SysUser user = sysUserMapper.selectById(review.getUserId());
        if (user != null) {
            reviewVO.setUserNickname(user.getNickname());
            reviewVO.setUserAvatar(user.getAvatar());
        }

        return reviewVO;
    }
}
