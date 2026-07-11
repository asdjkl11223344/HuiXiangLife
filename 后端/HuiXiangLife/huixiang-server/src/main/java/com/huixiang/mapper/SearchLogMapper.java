package com.huixiang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huixiang.entity.SearchLog;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {

    @Select("""
            SELECT keyword
            FROM search_log
            WHERE deleted = 0
              AND search_time >= #{beginTime}
            GROUP BY keyword
            ORDER BY COUNT(*) DESC, MAX(search_time) DESC
            LIMIT #{limit}
            """)
    List<String> selectHotKeywords(@Param("beginTime") LocalDateTime beginTime,
                                   @Param("limit") Integer limit);
}