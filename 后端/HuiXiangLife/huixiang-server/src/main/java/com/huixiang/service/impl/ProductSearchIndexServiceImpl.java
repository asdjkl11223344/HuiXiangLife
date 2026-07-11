package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huixiang.constant.ProductConstant;
import com.huixiang.entity.Merchant;
import com.huixiang.entity.Product;
import com.huixiang.exception.BusinessException;
import com.huixiang.mapper.MerchantMapper;
import com.huixiang.mapper.ProductMapper;
import com.huixiang.search.document.ProductDocument;
import com.huixiang.search.repository.ProductSearchRepository;
import com.huixiang.service.ProductSearchIndexService;
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
public class ProductSearchIndexServiceImpl implements ProductSearchIndexService {

    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<Long> searchProductIdsByKeyword(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        int size = Math.max(limit, 1);
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword.trim())
                            .fields("name^4", "subTitle^2", "content", "merchantName^2")))
                    .withPageable(PageRequest.of(0, size))
                    .build();
            SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
            List<Long> productIds = new ArrayList<>();
            for (SearchHit<ProductDocument> searchHit : searchHits) {
                if (searchHit.getContent() != null && searchHit.getContent().getId() != null) {
                    productIds.add(searchHit.getContent().getId());
                }
            }
            return productIds;
        } catch (Exception e) {
            log.warn("Elasticsearch 商品搜索失败, keyword={}", keyword, e);
            return null;
        }
    }

    @Override
    public void syncProduct(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            Product product = productMapper.selectById(productId);
            if (product == null || !ProductConstant.STATUS_ON_SHELF.equals(product.getStatus())) {
                productSearchRepository.deleteById(productId);
                return;
            }
            productSearchRepository.save(buildDocument(product));
        } catch (Exception e) {
            log.warn("同步商品搜索索引失败, productId={}", productId, e);
        }
    }

    @Override
    public void deleteProduct(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            productSearchRepository.deleteById(productId);
        } catch (Exception e) {
            log.warn("删除商品搜索索引失败, productId={}", productId, e);
        }
    }

    @Override
    public Integer rebuildAll() {
        try {
            LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Product::getStatus, ProductConstant.STATUS_ON_SHELF);
            List<Product> products = productMapper.selectList(queryWrapper);
            productSearchRepository.deleteAll();
            if (products.isEmpty()) {
                return 0;
            }
            List<ProductDocument> documents = products.stream()
                    .map(this::buildDocument)
                    .filter(Objects::nonNull)
                    .toList();
            productSearchRepository.saveAll(documents);
            return documents.size();
        } catch (Exception e) {
            log.warn("重建商品搜索索引失败", e);
            throw new BusinessException("Elasticsearch 未启动或不可用，无法重建商品搜索索引");
        }
    }

    private ProductDocument buildDocument(Product product) {
        if (product == null || product.getId() == null) {
            return null;
        }
        ProductDocument document = new ProductDocument();
        document.setId(product.getId());
        document.setName(product.getName());
        document.setSubTitle(product.getSubTitle());
        document.setContent(product.getContent());
        if (product.getMerchantId() != null) {
            Merchant merchant = merchantMapper.selectById(product.getMerchantId());
            if (merchant != null) {
                document.setMerchantName(merchant.getName());
            }
        }
        return document;
    }
}
