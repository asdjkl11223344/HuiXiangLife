package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.CouponReceiveDTO;
import com.huixiang.query.CouponQuery;
import com.huixiang.query.UserCouponQuery;
import com.huixiang.vo.CouponVO;
import com.huixiang.vo.UserCouponVO;
import jakarta.validation.Valid;

public interface CouponService {
    Page<CouponVO> page(CouponQuery couponQuery);

    Long receive(@Valid CouponReceiveDTO couponReceiveDTO);

    Page<UserCouponVO> myPage(UserCouponQuery userCouponQuery);
}