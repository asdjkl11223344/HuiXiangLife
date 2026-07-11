package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.CouponCreateDTO;
import com.huixiang.dto.CouponUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.CouponQuery;
import com.huixiang.result.Result;
import com.huixiang.service.CouponTemplateService;
import com.huixiang.vo.CouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/coupon-template")
@RequiredArgsConstructor
public class AdminCouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @GetMapping("/page")
    public Result<Page<CouponVO>> page(CouponQuery couponQuery) {
        Page<CouponVO> pageResult = couponTemplateService.page(couponQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<CouponVO> detail(@PathVariable Long id) {
        CouponVO couponVO = couponTemplateService.detail(id);
        return Result.success(couponVO);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CouponCreateDTO couponCreateDTO) {
        Long id = couponTemplateService.create(couponCreateDTO);
        return Result.success(id);
    }

    @PutMapping
    public Result<Boolean> update(@Valid @RequestBody CouponUpdateDTO couponUpdateDTO) {
        Boolean result = couponTemplateService.update(couponUpdateDTO);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateDTO statusUpdateDTO) {
        Boolean result = couponTemplateService.updateStatus(id, statusUpdateDTO);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        Boolean result = couponTemplateService.delete(id);
        return Result.success(result);
    }
}