package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.ProductCreateDTO;
import com.huixiang.dto.ProductUpdateDTO;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.ProductQuery;
import com.huixiang.vo.ProductDetailVO;
import com.huixiang.vo.SeckillAdminStatusVO;

import java.util.List;

public interface ProductService {

    /**
     * 用户端商品分页
     */
    Page<ProductDetailVO> page(ProductQuery productQuery);

    /**
     * 用户端商品详情
     */
    ProductDetailVO detail(Long id);

    /**
     * 管理端商品分页
     */
    Page<ProductDetailVO> adminPage(ProductQuery productQuery);

    /**
     * 管理端商品详情
     */
    ProductDetailVO adminDetail(Long id);

    /**
     * 新增商品
     */
    Long create(ProductCreateDTO productCreateDTO);

    /**
     * 修改商品
     */
    Boolean update(ProductUpdateDTO productUpdateDTO);

    /**
     * 修改商品状态
     */
    Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO);

    /**
     * 删除商品
     */
    Boolean delete(Long id);

    /**
     * 预热秒杀库存到 Redis
     */
    Boolean preheatSeckillStock(Long id);

    /**
     * 批量预热秒杀库存到 Redis
     */
    Integer batchPreheatSeckillStock(List<Long> ids);

    /**
     * 重置秒杀 Redis 状态
     */
    Boolean resetSeckillStock(Long id);

    /**
     * 批量重置秒杀 Redis 状态
     */
    Integer batchResetSeckillStock(List<Long> ids);

    /**
     * 查询秒杀调试状态
     */
    SeckillAdminStatusVO getSeckillAdminStatus(Long id, Long userId);

    /**
     * 手动触发即将开始商品的秒杀库存预热
     */
    Integer triggerUpcomingSeckillPreheat(Integer advanceMinutes);

    /**
     * 同步单个商品搜索索引
     */
    Boolean syncSearchIndex(Long id);

    /**
     * 全量重建商品搜索索引
     */
    Integer rebuildSearchIndex();

    List<ProductDetailVO> recommend(Integer limit);

    /**
     * 删除商品相关读缓存
     */
    void evictProductReadCaches(Long productId);
}
