package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.ProductConstant;
import com.huixiang.dto.ProductCreateDTO;
import com.huixiang.dto.ProductUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.Product;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.query.ProductQuery;
import com.huixiang.service.ProductService;
import com.huixiang.service.ProductSearchIndexService;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.ProductDetailVO;
import com.huixiang.vo.SeckillAdminStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
public class ProductServiceImpl implements ProductService {

    private static final int DEFAULT_SECKILL_PREHEAT_ADVANCE_MINUTES = 30;
    private static final int MAX_SEARCH_MATCH_COUNT = 1000;

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final ProductSearchIndexService productSearchIndexService;
    private final SeckillService seckillService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<ProductDetailVO> page(ProductQuery productQuery) {
        if (StringUtils.hasText(productQuery.getKeyword())) {
            Page<ProductDetailVO> searchPage = searchPageByKeyword(productQuery);
            if (searchPage != null) {
                return searchPage;
            }
        }
        return pageByDatabase(productQuery);
    }

    private Page<ProductDetailVO> pageByDatabase(ProductQuery productQuery) {
        String homePageCacheKey = null;
        if (shouldUseHomeProductPageCache(productQuery)) {
            homePageCacheKey = CacheConstant.HOME_PRODUCT_PAGE_KEY_PREFIX + productQuery.getPageSize();
            Page<ProductDetailVO> cachePage = getHomeProductPageCache(homePageCacheKey);
            if (cachePage != null) {
                return cachePage;
            }
        }
        Page<Product> page = new Page<>(productQuery.getPageNo(), productQuery.getPageSize());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductConstant.STATUS_ON_SHELF);
        if (productQuery.getMerchantId() != null) {
            queryWrapper.eq(Product::getMerchantId, productQuery.getMerchantId());
        }
        if (StringUtils.hasText(productQuery.getKeyword())) {
            queryWrapper.like(Product::getName, productQuery.getKeyword());
        }
        if (productQuery.getMinSalePrice() != null) {
            queryWrapper.ge(Product::getSalePrice, productQuery.getMinSalePrice());
        }
        if (productQuery.getMaxSalePrice() != null) {
            queryWrapper.le(Product::getSalePrice, productQuery.getMaxSalePrice());
        }
        queryWrapper.orderByDesc(Product::getCreateTime);
        Page<Product> productPage = productMapper.selectPage(page, queryWrapper);
        List<ProductDetailVO> records = productPage.getRecords()
                .stream()
                .map(this::buildProductDetailVO)
                .toList();
        Page<ProductDetailVO> resultPage = new Page<>(
                productPage.getCurrent(),
                productPage.getSize()
        );
        resultPage.setTotal(productPage.getTotal());
        resultPage.setRecords(records);
        if (homePageCacheKey != null) {
            long ttl = CacheConstant.HOME_PRODUCT_PAGE_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.HOME_PRODUCT_PAGE_TTL_RANDOM_BOUND_MINUTES + 1);
            setHomeProductPageCache(homePageCacheKey, resultPage, ttl);
        }
        return resultPage;
    }

    @Override
    public ProductDetailVO detail(Long id) {
        String key = CacheConstant.PRODUCT_DETAIL_KEY_PREFIX + id;
        Object value = getProductDetailCache(key);
        if (value instanceof ProductDetailVO productDetailVO){
            return productDetailVO;
        }
        if (CacheConstant.PRODUCT_DETAIL_NULL_VALUE.equals(value)){
            throw new NotFoundException("商品不存在");
        }
        Product product = productMapper.selectById(id);
        if (product == null || !ProductConstant.STATUS_ON_SHELF.equals(product.getStatus())) {
            setProductDetailNullCache(key);
            throw new NotFoundException("商品不存在");
        }
        ProductDetailVO productDetailVO = buildProductDetailVO(product);
        long ttl=CacheConstant.PRODUCT_DETAIL_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.PRODUCT_DETAIL_TTL_RANDOM_BOUND_MINUTES+1);
        setProductDetailCache(key, productDetailVO, ttl);
        return productDetailVO;
    }

    @Override
    public List<ProductDetailVO> recommend(Integer limit) {
        int size=limit==null?10:limit;
        if (size<1){
            size=1;
        }
        if (size>50){
            size=50;
        }
        String key = CacheConstant.PRODUCT_RECOMMEND_KEY_PREFIX + size;
        List<ProductDetailVO> cacheList = getRecommendCache(key);
        if (cacheList!=null){
            return cacheList;
        }
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductConstant.STATUS_ON_SHELF);
        queryWrapper.orderByDesc(Product::getSoldCount);
        queryWrapper.orderByDesc(Product::getCreateTime);
        queryWrapper.last("limit " + size);
        List<ProductDetailVO> result = productMapper.selectList(queryWrapper)
                .stream()
                .map(this::buildProductDetailVO)
                .toList();
        long ttl=CacheConstant.PRODUCT_RECOMMEND_TTL_MINUTES+ThreadLocalRandom.current().nextInt(
                CacheConstant.PRODUCT_RECOMMEND_TTL_RANDOM_BOUND_MINUTES+1);
        setRecommendCache(key,result,ttl);
        return result;
    }

    @Override
    public Page<ProductDetailVO> adminPage(ProductQuery productQuery) {
        Page<Product> page = new Page<>(productQuery.getPageNo(), productQuery.getPageSize());
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        if (productQuery.getMerchantId() != null) {
            queryWrapper.eq(Product::getMerchantId, productQuery.getMerchantId());
        }
        if (StringUtils.hasText(productQuery.getKeyword())) {
            queryWrapper.like(Product::getName, productQuery.getKeyword());
        }
        if (productQuery.getStatus() != null) {
            queryWrapper.eq(Product::getStatus, productQuery.getStatus());
        }
        if (productQuery.getMinSalePrice() != null) {
            queryWrapper.ge(Product::getSalePrice, productQuery.getMinSalePrice());
        }
        if (productQuery.getMaxSalePrice() != null) {
            queryWrapper.le(Product::getSalePrice, productQuery.getMaxSalePrice());
        }
        queryWrapper.orderByDesc(Product::getCreateTime);
        Page<Product> productPage = productMapper.selectPage(page, queryWrapper);
        List<ProductDetailVO> records = productPage.getRecords()
                .stream()
                .map(this::buildProductDetailVO)
                .toList();
        Page<ProductDetailVO> resultPage = new Page<>(
                productPage.getCurrent(),
                productPage.getSize()
        );
        resultPage.setTotal(productPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public ProductDetailVO adminDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return buildProductDetailVO(product);
    }

    @Override
    public Long create(ProductCreateDTO productCreateDTO) {
        Merchant merchant = merchantMapper.selectById(productCreateDTO.getMerchantId());
        if (merchant == null) {
            throw new NotFoundException("商户不存在");
        }
        Product product = new Product();
        product.setMerchantId(productCreateDTO.getMerchantId());
        product.setName(productCreateDTO.getName());
        product.setCoverUrl(productCreateDTO.getCoverUrl());
        product.setContent(productCreateDTO.getContent());
        product.setSalePrice(productCreateDTO.getSalePrice());
        product.setStock(productCreateDTO.getStock());
        product.setSoldCount(0);
        product.setStatus(ProductConstant.STATUS_ON_SHELF);
        productMapper.insert(product);
        productSearchIndexService.syncProduct(product.getId());
        evictProductReadCaches(product.getId());
        return product.getId();
    }

    @Override
    public Boolean update(ProductUpdateDTO productUpdateDTO) {
        Product product = productMapper.selectById(productUpdateDTO.getId());
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        if (productUpdateDTO.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(productUpdateDTO.getMerchantId());
            if (merchant == null) {
                throw new NotFoundException("商户不存在");
            }
        }
        product.setMerchantId(productUpdateDTO.getMerchantId());
        product.setName(productUpdateDTO.getName());
        product.setCoverUrl(productUpdateDTO.getCoverUrl());
        product.setContent(productUpdateDTO.getContent());
        product.setSalePrice(productUpdateDTO.getSalePrice());
        product.setStock(productUpdateDTO.getStock());
        product.setStatus(productUpdateDTO.getStatus());
        productMapper.updateById(product);
        productSearchIndexService.syncProduct(product.getId());
        evictProductReadCaches(product.getId());
        return true;
    }

    @Override
    public Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO) {
        if (!ProductConstant.STATUS_ON_SHELF.equals(statusUpdateDTO.getStatus())
                && !ProductConstant.STATUS_OFF_SHELF.equals(statusUpdateDTO.getStatus())) {
            throw new ParameterException("商品状态不正确");
        }
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        product.setStatus(statusUpdateDTO.getStatus());
        productMapper.updateById(product);
        productSearchIndexService.syncProduct(product.getId());
        evictProductReadCaches(product.getId());
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        productMapper.deleteById(id);
        productSearchIndexService.deleteProduct(id);
        evictProductReadCaches(product.getId());
        return true;
    }

    @Override
    public Boolean preheatSeckillStock(Long id) {
        Product product = getProductForSeckill(id);
        validateProductForSeckillPreheat(product);
        seckillService.preheatStock(product);
        return true;
    }

    @Override
    public Integer batchPreheatSeckillStock(List<Long> ids) {
        List<Long> productIds = normalizeProductIds(ids);
        for (Long productId : productIds) {
            Product product = getProductForSeckill(productId);
            validateProductForSeckillPreheat(product);
            seckillService.preheatStock(product);
        }
        return productIds.size();
    }

    @Override
    public Boolean resetSeckillStock(Long id) {
        Product product = getProductForSeckill(id);
        validateProductForSeckillReset(product);
        seckillService.resetStock(product);
        return true;
    }

    @Override
    public Integer batchResetSeckillStock(List<Long> ids) {
        List<Long> productIds = normalizeProductIds(ids);
        for (Long productId : productIds) {
            Product product = getProductForSeckill(productId);
            validateProductForSeckillReset(product);
            seckillService.resetStock(product);
        }
        return productIds.size();
    }

    @Override
    public SeckillAdminStatusVO getSeckillAdminStatus(Long id, Long userId) {
        Product product = getProductForSeckill(id);
        return seckillService.getAdminStatus(product.getId(), userId);
    }

    @Override
    public Integer triggerUpcomingSeckillPreheat(Integer advanceMinutes) {
        int minutes = advanceMinutes == null
                ? DEFAULT_SECKILL_PREHEAT_ADVANCE_MINUTES
                : Math.max(advanceMinutes, 1);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(minutes);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductConstant.STATUS_ON_SHELF)
                .isNotNull(Product::getStartTime)
                .gt(Product::getStartTime, now)
                .le(Product::getStartTime, deadline)
                .and(wrapper -> wrapper.isNull(Product::getEndTime).or().gt(Product::getEndTime, now));
        List<Product> products = productMapper.selectList(queryWrapper);
        if (products.isEmpty()) {
            return 0;
        }
        for (Product product : products) {
            validateProductForSeckillPreheat(product);
            seckillService.preheatStock(product);
        }
        return products.size();
    }

    @Override
    public Boolean syncSearchIndex(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        productSearchIndexService.syncProduct(id);
        return true;
    }

    @Override
    public Integer rebuildSearchIndex() {
        return productSearchIndexService.rebuildAll();
    }

    @Override
    public void evictProductReadCaches(Long productId) {
        deleteProductDetailCache(productId);
        deleteProductRecommendCache();
        deleteHomeProductPageCache();
        deleteHomeAggregateCache();
    }

    private Product getProductForSeckill(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        return product;
    }

    private Page<ProductDetailVO> searchPageByKeyword(ProductQuery productQuery) {
        List<Long> matchedProductIds = productSearchIndexService.searchProductIdsByKeyword(
                productQuery.getKeyword(),
                MAX_SEARCH_MATCH_COUNT
        );
        if (matchedProductIds == null) {
            return null;
        }
        Page<ProductDetailVO> resultPage = new Page<>(productQuery.getPageNo(), productQuery.getPageSize());
        if (matchedProductIds.isEmpty()) {
            resultPage.setTotal(0);
            resultPage.setRecords(List.of());
            return resultPage;
        }
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getStatus, ProductConstant.STATUS_ON_SHELF)
                .in(Product::getId, matchedProductIds);
        if (productQuery.getMerchantId() != null) {
            queryWrapper.eq(Product::getMerchantId, productQuery.getMerchantId());
        }
        if (productQuery.getMinSalePrice() != null) {
            queryWrapper.ge(Product::getSalePrice, productQuery.getMinSalePrice());
        }
        if (productQuery.getMaxSalePrice() != null) {
            queryWrapper.le(Product::getSalePrice, productQuery.getMaxSalePrice());
        }
        List<Product> matchedProducts = new ArrayList<>(productMapper.selectList(queryWrapper));
        if (matchedProducts.isEmpty()) {
            resultPage.setTotal(0);
            resultPage.setRecords(List.of());
            return resultPage;
        }
        Map<Long, Integer> productOrderMap = new HashMap<>(matchedProductIds.size());
        for (int i = 0; i < matchedProductIds.size(); i++) {
            productOrderMap.put(matchedProductIds.get(i), i);
        }
        matchedProducts.sort(Comparator.comparingInt(product -> productOrderMap.getOrDefault(product.getId(), Integer.MAX_VALUE)));
        int start = Math.max((productQuery.getPageNo() - 1) * productQuery.getPageSize(), 0);
        int end = Math.min(start + productQuery.getPageSize(), matchedProducts.size());
        List<ProductDetailVO> records = start >= matchedProducts.size()
                ? List.of()
                : matchedProducts.subList(start, end).stream().map(this::buildProductDetailVO).toList();
        resultPage.setTotal(matchedProducts.size());
        resultPage.setRecords(records);
        return resultPage;
    }

    private List<Long> normalizeProductIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ParameterException("商品ID不能为空");
        }
        List<Long> productIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            throw new ParameterException("商品ID不能为空");
        }
        return productIds;
    }

    private void validateProductForSeckillPreheat(Product product) {
        if (!ProductConstant.STATUS_ON_SHELF.equals(product.getStatus())) {
            throw new BusinessException("商品未上架，不能预热秒杀库存");
        }
        if (product.getEndTime() != null && !product.getEndTime().isAfter(java.time.LocalDateTime.now())) {
            throw new BusinessException("秒杀活动已结束，不能预热库存");
        }
        validateProductStock(product);
    }

    private void validateProductForSeckillReset(Product product) {
        validateProductStock(product);
    }

    private void validateProductStock(Product product) {
        if (product.getStock() != null && product.getStock() < 0) {
            throw new ParameterException("商品库存不正确");
        }
    }

    private ProductDetailVO buildProductDetailVO(Product product) {
        ProductDetailVO productDetailVO = new ProductDetailVO();
        productDetailVO.setId(product.getId());
        productDetailVO.setMerchantId(product.getMerchantId());
        productDetailVO.setName(product.getName());
        productDetailVO.setSubTitle(product.getSubTitle());
        productDetailVO.setContent(product.getContent());
        productDetailVO.setCoverUrl(product.getCoverUrl());
        productDetailVO.setOriginPrice(product.getOriginPrice());
        productDetailVO.setSalePrice(product.getSalePrice());
        productDetailVO.setStock(product.getStock());
        productDetailVO.setSoldCount(product.getSoldCount());
        productDetailVO.setStatus(product.getStatus());
        productDetailVO.setStartTime(product.getStartTime());
        productDetailVO.setEndTime(product.getEndTime());

        if (product.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(product.getMerchantId());
            if (merchant != null) {
                productDetailVO.setMerchantName(merchant.getName());
            }
        }

        return productDetailVO;
    }

    private void deleteProductDetailCache(Long productId) {
        if (productId == null) {
            return;
        }
        String key = CacheConstant.PRODUCT_DETAIL_KEY_PREFIX + productId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除商品详情缓存失败, key={}", key, e);
        }
    }

    private Object getProductDetailCache(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读取商品详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setProductDetailNullCache(String key) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    CacheConstant.PRODUCT_DETAIL_NULL_VALUE,
                    CacheConstant.PRODUCT_DETAIL_NULL_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入商品详情空缓存失败, key={}", key, e);
        }
    }

    private void setProductDetailCache(String key, ProductDetailVO productDetailVO, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, productDetailVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商品详情缓存失败, key={}", key, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProductDetailVO> getRecommendCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof List<?>) {
                return (List<ProductDetailVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取商品推荐缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setRecommendCache(String key, List<ProductDetailVO> list, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, list, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商品推荐缓存失败, key={}", key, e);
        }
    }

    private void deleteProductRecommendCache() {
        try {
            Set<String> keys = redisTemplate.keys(CacheConstant.PRODUCT_RECOMMEND_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除商品推荐缓存失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Page<ProductDetailVO> getHomeProductPageCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Page<?>) {
                return (Page<ProductDetailVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取商品首屏分页缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setHomeProductPageCache(String key, Page<ProductDetailVO> page, long ttl) {
        try {
            redisTemplate.opsForValue().set(key, page, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入商品首屏分页缓存失败, key={}", key, e);
        }
    }

    private void deleteHomeProductPageCache() {
        try {
            Set<String> keys = redisTemplate.keys(CacheConstant.HOME_PRODUCT_PAGE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("删除商品首屏分页缓存失败", e);
        }
    }

    private void deleteHomeAggregateCache() {
        try {
            redisTemplate.delete(CacheConstant.HOME_AGGREGATE_KEY);
        } catch (Exception e) {
            log.warn("删除首页聚合缓存失败, key={}", CacheConstant.HOME_AGGREGATE_KEY, e);
        }
    }

    private boolean shouldUseHomeProductPageCache(ProductQuery productQuery) {
        return productQuery != null
                && Integer.valueOf(1).equals(productQuery.getPageNo())
                && productQuery.getMerchantId() == null
                && !StringUtils.hasText(productQuery.getKeyword())
                && productQuery.getMinSalePrice() == null
                && productQuery.getMaxSalePrice() == null;
    }
}
