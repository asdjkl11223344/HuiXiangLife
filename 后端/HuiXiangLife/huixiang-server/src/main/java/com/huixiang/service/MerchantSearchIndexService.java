package com.huixiang.service;

import java.util.List;

public interface MerchantSearchIndexService {

    /**
     * 按关键词从 Elasticsearch 召回商户 ID
     */
    List<Long> searchMerchantIdsByKeyword(String keyword, int limit);

    /**
     * 同步单个商户到 Elasticsearch 索引
     */
    void syncMerchant(Long merchantId);

    /**
     * 删除单个商户索引
     */
    void deleteMerchant(Long merchantId);

    /**
     * 全量重建商户搜索索引
     */
    Integer rebuildAll();
}
