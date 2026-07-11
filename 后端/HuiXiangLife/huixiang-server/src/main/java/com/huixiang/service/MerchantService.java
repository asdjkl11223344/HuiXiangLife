package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.MerchantCreateDTO;
import com.huixiang.dto.MerchantUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.MerchantQuery;
import com.huixiang.vo.MerchantDetailVO;

public interface MerchantService {

    Page<MerchantDetailVO> page(MerchantQuery merchantQuery);

    MerchantDetailVO detail(Long id);

    Page<MerchantDetailVO> adminPage(MerchantQuery merchantQuery);

    MerchantDetailVO adminDetail(Long id);

    Long create(MerchantCreateDTO merchantCreateDTO);

    Boolean update(MerchantUpdateDTO merchantUpdateDTO);

    Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO);

    /**
     * 删除商户
     */
    Boolean delete(Long id);

    /**
     * 同步单个商户搜索索引
     */
    Boolean syncSearchIndex(Long id);

    /**
     * 全量重建商户搜索索引
     */
    Integer rebuildSearchIndex();
}
