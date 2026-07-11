package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.FavoriteConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.FavoriteCreateDTO;
import com.huixiang.entity.Favorite;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.Product;
import com.huixiang.exception.DataConflictException;
import com.huixiang.exception.ParameterException;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.mapper.FavoriteMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.query.FavoriteQuery;
import com.huixiang.service.FavoriteService;
import com.huixiang.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<FavoriteVO> page(FavoriteQuery favoriteQuery) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        String favoritePageCacheKey = null;
        if (shouldUseFavoritePageCache(favoriteQuery)) {
            favoritePageCacheKey = buildFavoritePageCacheKey(currentId, favoriteQuery);
            Page<FavoriteVO> cachePage = getFavoritePageCache(favoritePageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<Favorite> page = new Page<>(favoriteQuery.getPageNo(), favoriteQuery.getPageSize());
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Favorite::getUserId,currentId);
        if (favoriteQuery.getTargetType() != null) {
            queryWrapper.eq(Favorite::getTargetType, favoriteQuery.getTargetType());
        }
        queryWrapper.orderByDesc(Favorite::getCreateTime);
        Page<Favorite> favoritePage = favoriteMapper.selectPage(page, queryWrapper);
        List<FavoriteVO> records = favoritePage.getRecords()
                .stream()
                .map(this::buildFavoriteVO)
                .toList();
        Page<FavoriteVO> resultPage = new Page<>(
                favoritePage.getCurrent(),
                favoritePage.getSize()
                );
        resultPage.setTotal(favoritePage.getTotal());
        resultPage.setRecords(records);
        if (favoritePageCacheKey != null) {
            long ttl = CacheConstant.FAVORITE_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.FAVORITE_PAGE_TTL_RANDOM_BOUND_MINUTES + 1);
            setFavoritePageCache(favoritePageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    public Long create(FavoriteCreateDTO favoriteCreateDTO) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        validateTargetType(favoriteCreateDTO.getTargetType());
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Favorite::getUserId,currentId)
                .eq(Favorite::getTargetId, favoriteCreateDTO.getTargetId())
                .eq(Favorite::getTargetType, favoriteCreateDTO.getTargetType());
        Favorite existFavorite = favoriteMapper.selectOne(queryWrapper);
        if (existFavorite!=null){
            throw new DataConflictException("请勿重复收藏");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(currentId);
        favorite.setTargetId(favoriteCreateDTO.getTargetId());
        favorite.setTargetType(favoriteCreateDTO.getTargetType());
        favoriteMapper.insert(favorite);
        deleteFavoritePageCache(currentId);
        return favorite.getId();
    }

    @Override
    public Boolean delete(Long targetId, Integer targetType) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        validateTargetType(targetType);
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Favorite::getUserId, currentId)
                .eq(Favorite::getTargetId, targetId)
                .eq(Favorite::getTargetType, targetType);
        favoriteMapper.delete(queryWrapper);
        deleteFavoritePageCache(currentId);
        return true;
    }

    private FavoriteVO buildFavoriteVO(Favorite favorite) {
        FavoriteVO favoriteVO = new FavoriteVO();
        favoriteVO.setId(favorite.getId());
        favoriteVO.setUserId(favorite.getUserId());
        favoriteVO.setTargetId(favorite.getTargetId());
        favoriteVO.setTargetType(favorite.getTargetType());
        favoriteVO.setCreateTime(favorite.getCreateTime());

        if (FavoriteConstant.TARGET_TYPE_MERCHANT.equals(favorite.getTargetType())) {
            Merchant merchant = merchantMapper.selectById(favorite.getTargetId());
            if (merchant != null) {
                favoriteVO.setTargetName(merchant.getName());
                favoriteVO.setTargetCoverUrl(merchant.getCoverUrl());
            }
        } else if (FavoriteConstant.TARGET_TYPE_PRODUCT.equals(favorite.getTargetType())) {
            Product product = productMapper.selectById(favorite.getTargetId());
            if (product != null) {
                favoriteVO.setTargetName(product.getName());
                favoriteVO.setTargetCoverUrl(product.getCoverUrl());
            }
        }

        return favoriteVO;
    }

    private void validateTargetType(Integer targetType) {
        if (!FavoriteConstant.TARGET_TYPE_MERCHANT.equals(targetType)
                && !FavoriteConstant.TARGET_TYPE_PRODUCT.equals(targetType)) {
            throw new ParameterException("收藏目标类型错误");
        }
    }

    @SuppressWarnings("unchecked")
    private Page<FavoriteVO> getFavoritePageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<FavoriteVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取用户端收藏分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setFavoritePageCache(String key, Page<FavoriteVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入用户端收藏分页缓存失败, key={}", key, e);
        }
    }

    private void deleteFavoritePageCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            Set<String> keys = redisTemplate.keys(buildFavoritePageCachePattern(userId));
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除用户端收藏分页缓存失败, userId={}", userId, e);
        }
    }

    private boolean shouldUseFavoritePageCache(FavoriteQuery favoriteQuery) {
        return favoriteQuery != null
                && Integer.valueOf(1).equals(favoriteQuery.getPageNo());
    }

    private String buildFavoritePageCacheKey(Long userId, FavoriteQuery favoriteQuery) {
        String targetType = favoriteQuery.getTargetType() == null
                ? "all"
                : String.valueOf(favoriteQuery.getTargetType());
        return CacheConstant.FAVORITE_PAGE_KEY_PREFIX
                + "user:" + userId
                + ":size:" + favoriteQuery.getPageSize()
                + ":type:" + targetType;
    }

    private String buildFavoritePageCachePattern(Long userId) {
        return CacheConstant.FAVORITE_PAGE_KEY_PREFIX
                + "user:" + userId
                + ":*";
    }
}
