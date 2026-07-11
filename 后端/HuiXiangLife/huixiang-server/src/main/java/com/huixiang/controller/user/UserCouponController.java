package com.huixiang.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.CouponReceiveDTO;
import com.huixiang.query.CouponQuery;
import com.huixiang.query.UserCouponQuery;
import com.huixiang.result.Result;
import com.huixiang.service.CouponService;
import com.huixiang.vo.CouponVO;
import com.huixiang.vo.UserCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/coupon")
@RequiredArgsConstructor
public class UserCouponController {

    private final CouponService couponService;

    @GetMapping("/page")
    public Result<Page<CouponVO>> page(CouponQuery couponQuery) {
        Page<CouponVO> pageResult = couponService.page(couponQuery);
        return Result.success(pageResult);
    }

    @PostMapping("/receive")
    public Result<Long> receive(@Valid @RequestBody CouponReceiveDTO couponReceiveDTO) {
        Long id = couponService.receive(couponReceiveDTO);
        return Result.success(id);
    }

    @GetMapping("/my/page")
    public Result<Page<UserCouponVO>> myPage(UserCouponQuery userCouponQuery) {
        Page<UserCouponVO> pageResult = couponService.myPage(userCouponQuery);
        return Result.success(pageResult);
    }
}