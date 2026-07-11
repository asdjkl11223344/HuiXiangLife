package com.huixiang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huixiang.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecord> {
}