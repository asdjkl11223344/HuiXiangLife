package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.ReviewCreateDTO;
import com.huixiang.query.ReviewQuery;
import com.huixiang.vo.ReviewVO;
import jakarta.validation.Valid;

public interface ReviewService {
    
    Long create(@Valid ReviewCreateDTO reviewCreateDTO);

    Page<ReviewVO> page(ReviewQuery reviewQuery);

    Page<ReviewVO> adminPage(ReviewQuery reviewQuery);

    ReviewVO adminDetail(Long id);

    Boolean adminUpdateStatus(Long id, Integer status);
}