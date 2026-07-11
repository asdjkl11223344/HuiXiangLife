package com.huixiang.service.impl;

import com.huixiang.constant.CacheConstant;
import com.huixiang.query.MerchantQuery;
import com.huixiang.service.HomeService;
import com.huixiang.service.MerchantCategoryService;
import com.huixiang.service.MerchantService;
import com.huixiang.service.ProductService;
import com.huixiang.service.SearchService;
import com.huixiang.vo.HomeAggregateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

    private final MerchantCategoryService merchantCategoryService;
    private final ProductService productService;
    private final SearchService searchService;
    private final MerchantService merchantService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public HomeAggregateVO aggregate() {
        HomeAggregateVO cacheHome = getHomeAggregateCache();
        if (cacheHome != null) {
            return cacheHome;
        }
        HomeAggregateVO homeAggregateVO = new HomeAggregateVO();
        homeAggregateVO.setCategories(merchantCategoryService.list());
        homeAggregateVO.setRecommendProducts(productService.recommend(CacheConstant.HOME_AGGREGATE_RECOMMEND_LIMIT));
        homeAggregateVO.setHotKeywords(searchService.hotKeywords());

        MerchantQuery merchantQuery = new MerchantQuery();
        merchantQuery.setPageNo(1);
        merchantQuery.setPageSize(CacheConstant.HOME_AGGREGATE_MERCHANT_PAGE_SIZE);
        homeAggregateVO.setMerchants(merchantService.page(merchantQuery).getRecords());

        long ttl = CacheConstant.HOME_AGGREGATE_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.HOME_AGGREGATE_TTL_RANDOM_BOUND_MINUTES + 1);
        setHomeAggregateCache(homeAggregateVO, ttl);
        return homeAggregateVO;
    }

    private HomeAggregateVO getHomeAggregateCache() {
        try {
            Object value = redisTemplate.opsForValue().get(CacheConstant.HOME_AGGREGATE_KEY);
            if (value instanceof HomeAggregateVO homeAggregateVO) {
                return homeAggregateVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取首页聚合缓存失败, key={}", CacheConstant.HOME_AGGREGATE_KEY, e);
            return null;
        }
    }

    private void setHomeAggregateCache(HomeAggregateVO homeAggregateVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(
                    CacheConstant.HOME_AGGREGATE_KEY,
                    homeAggregateVO,
                    ttl,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入首页聚合缓存失败, key={}", CacheConstant.HOME_AGGREGATE_KEY, e);
        }
    }
}
