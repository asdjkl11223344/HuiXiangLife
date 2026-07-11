package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.MerchantConstant;
import com.huixiang.dto.MerchantCreateDTO;
import com.huixiang.dto.MerchantUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.MerchantCategory;
import com.huixiang.entity.OrderInfo;
import com.huixiang.entity.Product;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.mapper.MerchantCategoryMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.OrderInfoMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.query.MerchantQuery;
import com.huixiang.service.MerchantSearchIndexService;
import com.huixiang.service.MerchantService;
import com.huixiang.vo.MerchantDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final int MAX_SEARCH_MATCH_COUNT = 1000;

    private final MerchantMapper merchantMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;
    private final ProductMapper productMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final MerchantSearchIndexService merchantSearchIndexService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<MerchantDetailVO> page(MerchantQuery merchantQuery) {
        if (StringUtils.hasText(merchantQuery.getKeyword())) {
            Page<MerchantDetailVO> searchPage = searchPageByKeyword(merchantQuery);
            if (searchPage != null) {
                return searchPage;
            }
        }
        return pageByDatabase(merchantQuery);
    }

    private Page<MerchantDetailVO> pageByDatabase(MerchantQuery merchantQuery) {
        String homePageCacheKey = null;
        if (shouldUseHomeMerchantPageCache(merchantQuery)) {
            homePageCacheKey = CacheConstant.HOME_MERCHANT_PAGE_KEY_PREFIX + merchantQuery.getPageSize();
            Page<MerchantDetailVO> cachePage = getHomeMerchantPageCache(homePageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<Merchant> page = new Page<>(merchantQuery.getPageNo(), merchantQuery.getPageSize());
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Merchant::getStatus, MerchantConstant.STATUS_ENABLED);
        if (merchantQuery.getCategoryId() != null) {
            queryWrapper.eq(Merchant::getCategoryId, merchantQuery.getCategoryId());
        }
        if (StringUtils.hasText(merchantQuery.getKeyword())) {
            queryWrapper.like(Merchant::getName, merchantQuery.getKeyword());
        }
        if (merchantQuery.getMinScore() != null) {
            queryWrapper.ge(Merchant::getScore, merchantQuery.getMinScore());
        }
        if (merchantQuery.getMaxAvgPrice() != null) {
            queryWrapper.le(Merchant::getAvgPrice, merchantQuery.getMaxAvgPrice());
        }
        queryWrapper.orderByDesc(Merchant::getCreateTime);
        Page<Merchant> merchantPage = merchantMapper.selectPage(page, queryWrapper);
        List<MerchantDetailVO> records = merchantPage.getRecords()
                .stream()
                .map(this::buildMerchantDetailVO)
                .toList();
        Page<MerchantDetailVO> resultPage = new Page<>(
                merchantPage.getCurrent(),
                merchantPage.getSize()
        );
        resultPage.setTotal(merchantPage.getTotal());
        resultPage.setRecords(records);
        if (homePageCacheKey != null) {
            long ttl = CacheConstant.HOME_MERCHANT_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(
                    CacheConstant.HOME_MERCHANT_PAGE_TTL_RANDOM_BOUND_MINUTES + 1
            );
            setHomeMerchantPageCache(homePageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    public MerchantDetailVO detail(Long id) {
        String key = CacheConstant.MERCHANT_DETAIL_KEY_PREFIX + id;
        Object value = getMerchantDetailCache(key);
        if (value instanceof MerchantDetailVO merchantDetailVO) {
            return merchantDetailVO;
        }
        if (CacheConstant.PRODUCT_DETAIL_NULL_VALUE.equals(value)) {
            throw new NotFoundException("商户不存在");
        }
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant==null||!MerchantConstant.STATUS_ENABLED.equals(merchant.getStatus())){
            setMerchantDetailNullCache(key);
            throw new NotFoundException("商户不存在");
        }
        MerchantDetailVO merchantDetailVO = buildMerchantDetailVO(merchant);
        long ttl = CacheConstant.MERCHANT_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.MERCHANT_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setMerchantDetailCache(key, merchantDetailVO, ttl);
        return merchantDetailVO;
    }

    @Override
    public Page<MerchantDetailVO> adminPage(MerchantQuery merchantQuery) {
        Page<Merchant> page = new Page<>(merchantQuery.getPageNo(), merchantQuery.getPageSize());
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        if (merchantQuery.getCategoryId() != null) {
            queryWrapper.eq(Merchant::getCategoryId, merchantQuery.getCategoryId());
        }
        if (StringUtils.hasText(merchantQuery.getKeyword())) {
            queryWrapper.like(Merchant::getName, merchantQuery.getKeyword());
        }
        if (merchantQuery.getStatus()!=null){
            queryWrapper.eq(Merchant::getStatus, merchantQuery.getStatus());
        }
        if (merchantQuery.getMinScore()!=null){
            queryWrapper.gt(Merchant::getScore, merchantQuery.getMinScore());
        }
        if (merchantQuery.getMaxAvgPrice()!=null){
            queryWrapper.lt(Merchant::getAvgPrice, merchantQuery.getMaxAvgPrice());
        }
        queryWrapper.orderByDesc(Merchant::getCreateTime);
        Page<Merchant> merchantPage = merchantMapper.selectPage(page, queryWrapper);
        List<MerchantDetailVO> records = merchantPage.getRecords()
                .stream()
                .map(this::buildMerchantDetailVO)
                .toList();
        Page<MerchantDetailVO> resultPage = new Page<>(
                merchantPage.getCurrent(),
                merchantPage.getSize()
        );
        resultPage.setTotal(merchantPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public MerchantDetailVO adminDetail(Long id) {
        String key = CacheConstant.ADMIN_MERCHANT_DETAIL_KEY_PREFIX + id;
        MerchantDetailVO cacheMerchantDetail = getAdminMerchantDetailCache(key);
        if (cacheMerchantDetail != null) {
            return cacheMerchantDetail;
        }
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant==null){
            throw new NotFoundException("商户不存在");
        }
        MerchantDetailVO merchantDetailVO = buildMerchantDetailVO(merchant);
        long ttl = CacheConstant.ADMIN_MERCHANT_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.ADMIN_MERCHANT_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
        setAdminMerchantDetailCache(key, merchantDetailVO, ttl);
        return merchantDetailVO;
    }

    @Override
    public Long create(MerchantCreateDTO merchantCreateDTO) {
        MerchantCategory merchantCategory = merchantCategoryMapper.selectById(merchantCreateDTO.getCategoryId());
        if (merchantCategory==null){
            throw new NotFoundException("商户分类不存在");
        }
        Merchant merchant = new Merchant();
        merchant.setName(merchantCreateDTO.getName());
        merchant.setCategoryId(merchantCreateDTO.getCategoryId());
        merchant.setCoverUrl(merchantCreateDTO.getCoverUrl());
        merchant.setAddress(merchantCreateDTO.getAddress());
        merchant.setPhone(merchantCreateDTO.getPhone());
        merchant.setDescription(merchantCreateDTO.getDescription());
        merchant.setAvgPrice(merchantCreateDTO.getAvgPrice());
        merchant.setStatus(MerchantConstant.STATUS_ENABLED);
        merchantMapper.insert(merchant);
        merchantSearchIndexService.syncMerchant(merchant.getId());
        evictMerchantReadCaches(merchant.getId());
        return merchant.getId();
    }

    @Override
    public Boolean update(MerchantUpdateDTO merchantUpdateDTO) {
        Merchant merchant = merchantMapper.selectById(merchantUpdateDTO.getId());
        if (merchant==null){
            throw new NotFoundException("商户不存在");
        }
        if (merchantUpdateDTO.getCategoryId()!=null){
            MerchantCategory merchantCategory = merchantCategoryMapper.selectById(merchantUpdateDTO.getCategoryId());
            if (merchantCategory==null){
                throw new NotFoundException("商户分类不存在");
            }
        }
        merchant.setName(merchantUpdateDTO.getName());
        merchant.setCategoryId(merchantUpdateDTO.getCategoryId());
        merchant.setCoverUrl(merchantUpdateDTO.getCoverUrl());
        merchant.setAddress(merchantUpdateDTO.getAddress());
        merchant.setPhone(merchantUpdateDTO.getPhone());
        merchant.setDescription(merchantUpdateDTO.getDescription());
        merchant.setAvgPrice(merchantUpdateDTO.getAvgPrice());
        merchant.setStatus(merchantUpdateDTO.getStatus());
        merchantMapper.updateById(merchant);
        merchantSearchIndexService.syncMerchant(merchant.getId());
        evictMerchantReadCaches(merchant.getId());
        return true;
    }

    @Override
    public Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO) {
        if (!MerchantConstant.STATUS_ENABLED.equals(statusUpdateDTO.getStatus())
                && !MerchantConstant.STATUS_DISABLED.equals(statusUpdateDTO.getStatus())) {
            throw new ParameterException("商户状态不正确");
        }
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant==null){
            throw new NotFoundException("商户不存在");
        }
        merchant.setStatus(statusUpdateDTO.getStatus());
        merchantMapper.updateById(merchant);
        merchantSearchIndexService.syncMerchant(merchant.getId());
        evictMerchantReadCaches(merchant.getId());
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new NotFoundException("商户不存在");
        }
        validateMerchantCanDelete(id);
        merchantMapper.deleteById(id);
        merchantSearchIndexService.deleteMerchant(id);
        evictMerchantReadCaches(id);
        return true;
    }

    @Override
    public Boolean syncSearchIndex(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new NotFoundException("商户不存在");
        }
        merchantSearchIndexService.syncMerchant(id);
        return true;
    }

    @Override
    public Integer rebuildSearchIndex() {
        return merchantSearchIndexService.rebuildAll();
    }

    private Page<MerchantDetailVO> searchPageByKeyword(MerchantQuery merchantQuery) {
        List<Long> matchedMerchantIds = merchantSearchIndexService.searchMerchantIdsByKeyword(
                merchantQuery.getKeyword(),
                MAX_SEARCH_MATCH_COUNT
        );
        if (matchedMerchantIds == null) {
            return null;
        }
        Page<MerchantDetailVO> resultPage = new Page<>(merchantQuery.getPageNo(), merchantQuery.getPageSize());
        if (matchedMerchantIds.isEmpty()) {
            resultPage.setTotal(0);
            resultPage.setRecords(List.of());
            return resultPage;
        }
        LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Merchant::getStatus, MerchantConstant.STATUS_ENABLED)
                .in(Merchant::getId, matchedMerchantIds);
        if (merchantQuery.getCategoryId() != null) {
            queryWrapper.eq(Merchant::getCategoryId, merchantQuery.getCategoryId());
        }
        if (merchantQuery.getMinScore() != null) {
            queryWrapper.ge(Merchant::getScore, merchantQuery.getMinScore());
        }
        if (merchantQuery.getMaxAvgPrice() != null) {
            queryWrapper.le(Merchant::getAvgPrice, merchantQuery.getMaxAvgPrice());
        }
        List<Merchant> matchedMerchants = new ArrayList<>(merchantMapper.selectList(queryWrapper));
        if (matchedMerchants.isEmpty()) {
            resultPage.setTotal(0);
            resultPage.setRecords(List.of());
            return resultPage;
        }
        Map<Long, Integer> merchantOrderMap = new HashMap<>(matchedMerchantIds.size());
        for (int i = 0; i < matchedMerchantIds.size(); i++) {
            merchantOrderMap.put(matchedMerchantIds.get(i), i);
        }
        matchedMerchants.sort(Comparator.comparingInt(merchant ->
                merchantOrderMap.getOrDefault(merchant.getId(), Integer.MAX_VALUE)));
        int start = Math.max((merchantQuery.getPageNo() - 1) * merchantQuery.getPageSize(), 0);
        int end = Math.min(start + merchantQuery.getPageSize(), matchedMerchants.size());
        List<MerchantDetailVO> records = start >= matchedMerchants.size()
                ? List.of()
                : matchedMerchants.subList(start, end).stream().map(this::buildMerchantDetailVO).toList();
        resultPage.setTotal(matchedMerchants.size());
        resultPage.setRecords(records);
        return resultPage;
    }

    private void validateMerchantCanDelete(Long merchantId) {
        LambdaQueryWrapper<Product> productQueryWrapper = new LambdaQueryWrapper<>();
        productQueryWrapper.eq(Product::getMerchantId, merchantId);
        if (productMapper.selectCount(productQueryWrapper) > 0) {
            throw new BusinessException("该商户下仍有关联商品，不能删除");
        }

        LambdaQueryWrapper<OrderInfo> orderQueryWrapper = new LambdaQueryWrapper<>();
        orderQueryWrapper.eq(OrderInfo::getMerchantId, merchantId);
        if (orderInfoMapper.selectCount(orderQueryWrapper) > 0) {
            throw new BusinessException("该商户下仍有关联订单，不能删除");
        }
    }


    private MerchantDetailVO buildMerchantDetailVO(Merchant merchant) {
        MerchantDetailVO merchantDetailVO = new MerchantDetailVO();
        merchantDetailVO.setId(merchant.getId());
        merchantDetailVO.setName(merchant.getName());
        merchantDetailVO.setCategoryId(merchant.getCategoryId());
        merchantDetailVO.setCoverUrl(merchant.getCoverUrl());
        merchantDetailVO.setAddress(merchant.getAddress());
        merchantDetailVO.setPhone(merchant.getPhone());
        merchantDetailVO.setDescription(merchant.getDescription());
        merchantDetailVO.setScore(merchant.getScore());
        merchantDetailVO.setAvgPrice(merchant.getAvgPrice());
        merchantDetailVO.setStatus(merchant.getStatus());

        if (merchant.getCategoryId() != null) {
            MerchantCategory merchantCategory = merchantCategoryMapper.selectById(merchant.getCategoryId());
            if (merchantCategory != null) {
                merchantDetailVO.setCategoryName(merchantCategory.getName());
            }
        }

        return merchantDetailVO;
    }

    private void evictMerchantReadCaches(Long merchantId) {
        deleteMerchantDetailCache(merchantId);
        deleteHomeMerchantPageCache();
        deleteHomeAggregateCache();
    }

    private void deleteMerchantDetailCache(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            redisTemplate.delete(CacheConstant.MERCHANT_DETAIL_KEY_PREFIX + merchantId);
            redisTemplate.delete(CacheConstant.ADMIN_MERCHANT_DETAIL_KEY_PREFIX + merchantId);
        } catch (Exception e) {
            log.warn("删除商户详情缓存失败, merchantId={}", merchantId, e);
        }
    }

    private MerchantDetailVO getAdminMerchantDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof MerchantDetailVO merchantDetailVO) {
                return merchantDetailVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取管理端商户详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setAdminMerchantDetailCache(String key, MerchantDetailVO merchantDetailVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, merchantDetailVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入管理端商户详情缓存失败, key={}", key, e);
        }
    }

    private Object getMerchantDetailCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取商户详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setMerchantDetailNullCache(String key) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    CacheConstant.PRODUCT_DETAIL_NULL_VALUE,
                    CacheConstant.MERCHANT_DETAIL_NULL_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入商户详情空缓存失败, key={}", key, e);
        }
    }

    private void setMerchantDetailCache(String key, MerchantDetailVO merchantDetailVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, merchantDetailVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商户详情缓存失败, key={}", key, e);
        }
    }

    private boolean shouldUseHomeMerchantPageCache(MerchantQuery merchantQuery) {
        return merchantQuery != null
                && Integer.valueOf(1).equals(merchantQuery.getPageNo())
                && merchantQuery.getCategoryId() == null
                && !StringUtils.hasText(merchantQuery.getKeyword())
                && merchantQuery.getMinScore() == null
                && merchantQuery.getMaxAvgPrice() == null;
    }

    @SuppressWarnings("unchecked")
    private Page<MerchantDetailVO> getHomeMerchantPageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<MerchantDetailVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取商户首屏分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setHomeMerchantPageCache(String key, Page<MerchantDetailVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商户首屏分页缓存失败, key={}", key, e);
        }
    }

    private void deleteHomeMerchantPageCache() {
        try {
            Set<String> keys = redisTemplate.keys(CacheConstant.HOME_MERCHANT_PAGE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除商户首屏分页缓存失败", e);
        }
    }

    private void deleteHomeAggregateCache() {
        try {
            redisTemplate.delete(CacheConstant.HOME_AGGREGATE_KEY);
        } catch (Exception e) {
            log.warn("删除首页聚合缓存失败, key={}", CacheConstant.HOME_AGGREGATE_KEY, e);
        }
    }
}
