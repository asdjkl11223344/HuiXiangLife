package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huixiang.constant.MerchantConstant;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.MerchantCategory;
import com.huixiang.exception.BusinessException;
import com.huixiang.mapper.MerchantCategoryMapper;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.search.document.MerchantDocument;
import com.huixiang.search.repository.MerchantSearchRepository;
import com.huixiang.service.MerchantSearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantSearchIndexServiceImpl implements MerchantSearchIndexService {

    private final MerchantMapper merchantMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;
    private final MerchantSearchRepository merchantSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<Long> searchMerchantIdsByKeyword(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int size = Math.max(limit, 1);
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword.trim())
                            .fields("name^4", "description^2", "address", "categoryName^2")))
                    .withPageable(PageRequest.of(0, size))
                    .build();
            SearchHits<MerchantDocument> searchHits = elasticsearchOperations.search(query, MerchantDocument.class);
            List<Long> merchantIds = new ArrayList<>();
            for (SearchHit<MerchantDocument> searchHit : searchHits) {
                if (searchHit.getContent() != null && searchHit.getContent().getId() != null) {
                    merchantIds.add(searchHit.getContent().getId());
                }
            }
            return merchantIds;
        } catch (Exception e) {
            log.warn("Elasticsearch 商户搜索失败, keyword={}", keyword, e);
            return null;
        }
    }

    @Override
    public void syncMerchant(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            Merchant merchant = merchantMapper.selectById(merchantId);
            if (merchant == null || !MerchantConstant.STATUS_ENABLED.equals(merchant.getStatus())) {
                merchantSearchRepository.deleteById(merchantId);
                return;
            }
            merchantSearchRepository.save(buildDocument(merchant));
        } catch (Exception e) {
            log.warn("同步商户搜索索引失败, merchantId={}", merchantId, e);
        }
    }

    @Override
    public void deleteMerchant(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            merchantSearchRepository.deleteById(merchantId);
        } catch (Exception e) {
            log.warn("删除商户搜索索引失败, merchantId={}", merchantId, e);
        }
    }

    @Override
    public Integer rebuildAll() {
        try {
            LambdaQueryWrapper<Merchant> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Merchant::getStatus, MerchantConstant.STATUS_ENABLED);
            List<Merchant> merchants = merchantMapper.selectList(queryWrapper);
            merchantSearchRepository.deleteAll();
            if (merchants.isEmpty()) {
                return 0;
            }
            List<MerchantDocument> documents = merchants.stream()
                    .map(this::buildDocument)
                    .filter(Objects::nonNull)
                    .toList();
            merchantSearchRepository.saveAll(documents);
            return documents.size();
        } catch (Exception e) {
            log.warn("重建商户搜索索引失败", e);
            throw new BusinessException("Elasticsearch 未启动或不可用，无法重建商户搜索索引");
        }
    }

    private MerchantDocument buildDocument(Merchant merchant) {
        if (merchant == null || merchant.getId() == null) {
            return null;
        }
        MerchantDocument document = new MerchantDocument();
        document.setId(merchant.getId());
        document.setName(merchant.getName());
        document.setDescription(merchant.getDescription());
        document.setAddress(merchant.getAddress());
        if (merchant.getCategoryId() != null) {
            MerchantCategory merchantCategory = merchantCategoryMapper.selectById(merchant.getCategoryId());
            if (merchantCategory != null) {
                document.setCategoryName(merchantCategory.getName());
            }
        }
        return document;
    }
}
