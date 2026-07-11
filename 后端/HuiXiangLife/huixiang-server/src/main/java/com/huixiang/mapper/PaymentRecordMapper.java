package com.huixiang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huixiang.entity.PaymentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    @Insert({
            "<script>",
            "INSERT INTO payment_record (",
            "id, order_id, pay_channel, pay_status, transaction_no, callback_content, pay_time, create_time, update_time, deleted",
            ")",
            "SELECT",
            "#{id}, #{orderId}, #{payChannel}, #{payStatus}, #{transactionNo}, #{callbackContent}, #{payTime}, #{createTime}, #{updateTime}, #{deleted}",
            "FROM DUAL",
            "WHERE NOT EXISTS (",
            "SELECT 1 FROM payment_record",
            "WHERE order_id = #{orderId}",
            "AND pay_status = #{payStatus}",
            "AND deleted = 0",
            ")",
            "</script>"
    })
    int insertPendingIfAbsent(PaymentRecord paymentRecord);
}
