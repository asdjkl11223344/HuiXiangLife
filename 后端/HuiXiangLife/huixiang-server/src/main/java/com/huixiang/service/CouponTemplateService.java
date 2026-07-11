package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.CouponCreateDTO;
import com.huixiang.dto.CouponUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.CouponQuery;
import com.huixiang.vo.CouponVO;

public interface CouponTemplateService {

    Page<CouponVO> page(CouponQuery couponQuery);

    CouponVO detail(Long id);

    Long create(CouponCreateDTO couponCreateDTO);

    Boolean update(CouponUpdateDTO couponUpdateDTO);

    Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO);

    Boolean delete(Long id);
}