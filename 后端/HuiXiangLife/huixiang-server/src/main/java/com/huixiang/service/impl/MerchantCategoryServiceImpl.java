package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huixiang.constant.CacheConstant;
import com.huixiang.dto.MerchantCategoryCreateDTO;
import com.huixiang.dto.MerchantCategoryUpdateDTO;
import com.huixiang.entity.MerchantCategory;
import com.huixiang.exception.NotFoundException;
import com.huixiang.mapper.MerchantCategoryMapper;
import com.huixiang.service.MerchantCategoryService;
import com.huixiang.vo.MerchantCategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantCategoryServiceImpl implements MerchantCategoryService {

    private final MerchantCategoryMapper merchantCategoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<MerchantCategoryVO> list() {
        List<MerchantCategoryVO> cacheList = getMerchantCategoryListCache();
        if (cacheList != null) {
            return cacheList;
        }
        LambdaQueryWrapper<MerchantCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(MerchantCategory::getSort)
                .orderByAsc(MerchantCategory::getId);
        List<MerchantCategory> list = merchantCategoryMapper.selectList(queryWrapper);
        List<MerchantCategoryVO> result = list.stream()
                .map(this::buildMerchantCategoryVO)
                .toList();
        long ttl = CacheConstant.MERCHANT_CATEGORY_LIST_TTL_MINUTES
                + ThreadLocalRandom.current().nextInt(CacheConstant.MERCHANT_CATEGORY_LIST_TTL_RANDOM_BOUND_MINUTES + 1);
        setMerchantCategoryListCache(result, ttl);
        return result;
    }

    @Override
    public Long create(MerchantCategoryCreateDTO merchantCategoryCreateDTO) {
        MerchantCategory merchantCategory = new MerchantCategory();
        merchantCategory.setName(merchantCategoryCreateDTO.getName());
        merchantCategory.setSort(merchantCategoryCreateDTO.getSort() == null ? 0 : merchantCategoryCreateDTO.getSort());
        merchantCategory.setStatus(merchantCategoryCreateDTO.getStatus() == null ? 1 : merchantCategoryCreateDTO.getStatus());
        merchantCategoryMapper.insert(merchantCategory);
        deleteMerchantCategoryListCache();
        deleteHomeAggregateCache();
        return merchantCategory.getId();
    }

    @Override
    public Boolean update(MerchantCategoryUpdateDTO merchantCategoryUpdateDTO) {
        MerchantCategory merchantCategory = merchantCategoryMapper.selectById(merchantCategoryUpdateDTO.getId());
        if (merchantCategory == null){
            throw new NotFoundException("商户分类不存在");
        }
        merchantCategory.setName(merchantCategoryUpdateDTO.getName());
        merchantCategory.setSort(merchantCategoryUpdateDTO.getSort() == null ? 0 : merchantCategoryUpdateDTO.getSort());
        merchantCategory.setStatus(merchantCategoryUpdateDTO.getStatus() == null ? 1 : merchantCategoryUpdateDTO.getStatus());
        merchantCategoryMapper.updateById(merchantCategory);
        deleteMerchantCategoryListCache();
        deleteHomeAggregateCache();
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        MerchantCategory merchantCategory = merchantCategoryMapper.selectById(id);
        if (merchantCategory==null){
            throw new NotFoundException("商户分类不存在");
        }
        merchantCategoryMapper.deleteById(id);
        deleteMerchantCategoryListCache();
        deleteHomeAggregateCache();
        return true;
    }

    private MerchantCategoryVO buildMerchantCategoryVO(MerchantCategory merchantCategory) {
        MerchantCategoryVO merchantCategoryVO = new MerchantCategoryVO();
        merchantCategoryVO.setId(merchantCategory.getId());
        merchantCategoryVO.setName(merchantCategory.getName());
        merchantCategoryVO.setSort(merchantCategory.getSort());
        merchantCategoryVO.setStatus(merchantCategory.getStatus());
        return merchantCategoryVO;
    }

    @SuppressWarnings("unchecked")
    private List<MerchantCategoryVO> getMerchantCategoryListCache() {
        try {
            Object value = redisTemplate.opsForValue().get(CacheConstant.MERCHANT_CATEGORY_LIST_KEY);
            if (value instanceof List<?>) {
                return (List<MerchantCategoryVO>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取商户分类列表缓存失败, key={}", CacheConstant.MERCHANT_CATEGORY_LIST_KEY, e);
            return null;
        }
    }

    private void setMerchantCategoryListCache(List<MerchantCategoryVO> list, long ttl) {
        try {
            redisTemplate.opsForValue().set(
                    CacheConstant.MERCHANT_CATEGORY_LIST_KEY,
                    list,
                    ttl,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入商户分类列表缓存失败, key={}", CacheConstant.MERCHANT_CATEGORY_LIST_KEY, e);
        }
    }

    private void deleteMerchantCategoryListCache() {
        try {
            redisTemplate.delete(CacheConstant.MERCHANT_CATEGORY_LIST_KEY);
        } catch (Exception e) {
            log.warn("删除商户分类列表缓存失败, key={}", CacheConstant.MERCHANT_CATEGORY_LIST_KEY, e);
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
