package com.huixiang.vo;

import lombok.Data;

import java.util.List;

@Data
public class HomeAggregateVO {

    private List<MerchantCategoryVO> categories;

    private List<ProductDetailVO> recommendProducts;

    private List<String> hotKeywords;

    private List<MerchantDetailVO> merchants;
}
