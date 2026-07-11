package com.huixiang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huixiang.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("""
            UPDATE product
            SET stock = stock - #{quantity}
            WHERE id = #{productId}
              AND status = #{status}
              AND stock >= #{quantity}
              AND deleted = 0
            """)
    int deductStockIfEnough(@Param("productId") Long productId,
                            @Param("quantity") Integer quantity,
                            @Param("status") Integer status);

    @Update("""
            UPDATE product
            SET stock = stock + #{quantity}
            WHERE id = #{productId}
              AND deleted = 0
            """)
    int increaseStock(@Param("productId") Long productId,
                      @Param("quantity") Integer quantity);

    @Update("""
            UPDATE product
            SET sold_count = sold_count + #{quantity}
            WHERE id = #{productId}
              AND deleted = 0
            """)
    int increaseSoldCount(@Param("productId") Long productId,
                          @Param("quantity") Integer quantity);
}
