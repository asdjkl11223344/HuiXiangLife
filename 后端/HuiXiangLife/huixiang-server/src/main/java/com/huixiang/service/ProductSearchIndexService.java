package com.huixiang.service;

import java.util.List;

public interface ProductSearchIndexService {

    /**
     * 按关键词从 Elasticsearch 召回商品 ID
     */
    List<Long> searchProductIdsByKeyword(String keyword, int limit);

    /**
     * 同步单个商品到 Elasticsearch 索引
     */
    void syncProduct(Long productId);

    /**
     * 删除单个商品索引
     */
    void deleteProduct(Long productId);

    /**
     * 全量重建商品搜索索引
     */
    Integer rebuildAll();
}
