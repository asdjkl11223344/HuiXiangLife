package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.FavoriteCreateDTO;
import com.huixiang.query.FavoriteQuery;
import com.huixiang.vo.FavoriteVO;
import jakarta.validation.Valid;

public interface FavoriteService {

    Page<FavoriteVO> page(FavoriteQuery favoriteQuery);

    Long create(@Valid FavoriteCreateDTO favoriteCreateDTO);

    Boolean delete(Long targetId, Integer targetType);
}