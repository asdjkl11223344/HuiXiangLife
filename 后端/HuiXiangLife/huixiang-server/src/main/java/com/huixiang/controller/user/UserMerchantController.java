package com.huixiang.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.query.MerchantQuery;
import com.huixiang.result.Result;
import com.huixiang.service.MerchantService;
import com.huixiang.service.SearchService;
import com.huixiang.vo.MerchantDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/merchant")
@RequiredArgsConstructor
public class UserMerchantController {

    private final MerchantService merchantService;
    private final SearchService searchService;

    @GetMapping("/page")
    public Result<Page<MerchantDetailVO>> page(MerchantQuery merchantQuery) {
        Page<MerchantDetailVO> pageResult = merchantService.page(merchantQuery);
        searchService.recordKeyword(merchantQuery.getKeyword());
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<MerchantDetailVO> detail(@PathVariable Long id) {
        MerchantDetailVO merchantDetailVO = merchantService.detail(id);
        return Result.success(merchantDetailVO);
    }
}
