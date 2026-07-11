package com.huixiang.controller.user;

import com.huixiang.query.ProductQuery;
import com.huixiang.result.Result;
import com.huixiang.service.ProductService;
import com.huixiang.service.SearchService;
import com.huixiang.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/product")
@RequiredArgsConstructor
public class UserProductController {

    private final ProductService productService;
    private final SearchService searchService;

    @GetMapping("/page")
    public Result<Page<ProductDetailVO>> page(ProductQuery productQuery){
        Page<ProductDetailVO> page=productService.page(productQuery);
        searchService.recordKeyword(productQuery.getKeyword());
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id){
        ProductDetailVO productDetailVO=productService.detail(id);
        return Result.success(productDetailVO);
    }

    @GetMapping("/recommend")
    public Result<List<ProductDetailVO>> recommend(@RequestParam(required = false) Integer limit){
        List<ProductDetailVO> list=productService.recommend(limit);
        return Result.success(list);
    }
}
