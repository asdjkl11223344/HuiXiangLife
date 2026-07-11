package com.huixiang.service.impl;

import com.huixiang.constant.CacheConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.entity.SearchLog;
import com.huixiang.mapper.SearchLogMapper;
import com.huixiang.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final SearchLogMapper searchLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<String> hotKeywords() {
        String key = CacheConstant.SEARCH_HOT_KEY;
        List<String> cacheList = getHotKeywordsCache(key);
        if (cacheList!=null){
            return cacheList;
        }
        LocalDateTime beginTime = LocalDateTime.now().minusDays(7);
        List<String> list = searchLogMapper.selectHotKeywords(beginTime, 10);
        setHotKeywordsCache(key,list);
        return list;
    }

    @Override
    public void recordKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            SearchLog searchLog = new SearchLog();
            searchLog.setUserId(BaseContext.getCurrentId());
            searchLog.setKeyword(keyword.trim());
            searchLog.setSearchTime(LocalDateTime.now());
            searchLogMapper.insert(searchLog);
        } catch (Exception e) {
            log.warn("记录搜索关键词失败, keyword={}", keyword, e);
            return;
        }
        deleteHotKeywordsCache(CacheConstant.SEARCH_HOT_KEY);
        deleteHomeAggregateCache();
    }

    @SuppressWarnings("unchecked")
    private List<String> getHotKeywordsCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof List<?>) {
                return (List<String>) value;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取热门搜索词缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setHotKeywordsCache(String key, List<String> list) {
        try {
            redisTemplate.opsForValue().set(
                    key,
                    list,
                    CacheConstant.SEARCH_HOT_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.warn("写入热门搜索词缓存失败, key={}", key, e);
        }
    }

    private void deleteHotKeywordsCache(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除热门搜索词缓存失败, key={}", key, e);
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
