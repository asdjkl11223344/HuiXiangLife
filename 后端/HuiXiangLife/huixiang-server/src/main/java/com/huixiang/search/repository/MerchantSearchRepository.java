package com.huixiang.search.repository;

import com.huixiang.search.document.MerchantDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MerchantSearchRepository extends ElasticsearchRepository<MerchantDocument, Long> {
}
