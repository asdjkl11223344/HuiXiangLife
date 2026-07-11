package com.huixiang.service;

import java.util.List;

import com.huixiang.dto.MerchantCategoryCreateDTO;
import com.huixiang.dto.MerchantCategoryUpdateDTO;
import com.huixiang.vo.MerchantCategoryVO;

public interface MerchantCategoryService {

    List<MerchantCategoryVO> list();

    Long create(MerchantCategoryCreateDTO merchantCategoryCreateDTO);

    Boolean update(MerchantCategoryUpdateDTO merchantCategoryUpdateDTO);

    Boolean delete(Long id);
}