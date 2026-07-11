package com.huixiang.service;

import com.huixiang.dto.MqNotifyDTO;
import com.huixiang.dto.PayNotifyDTO;
import com.huixiang.vo.NotifyAckVO;
import jakarta.validation.Valid;

public interface NotifyService {

    Boolean payCallback(@Valid PayNotifyDTO payNotifyDTO);

    NotifyAckVO handleOrderTimeout(MqNotifyDTO mqNotifyDTO);

    NotifyAckVO handleCouponExpire(MqNotifyDTO mqNotifyDTO);

    NotifyAckVO handleOrderStatusSync(MqNotifyDTO mqNotifyDTO);
}