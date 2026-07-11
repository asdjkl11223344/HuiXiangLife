package com.huixiang.task;

import com.huixiang.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "huixiang.seckill.preheat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeckillPreheatTask {

    private final ProductService productService;

    @Value("${huixiang.seckill.preheat.advance-minutes:30}")
    private int advanceMinutes;

    @Scheduled(fixedDelayString = "${huixiang.seckill.preheat.fixed-delay-millis:60000}")
    public void preheatUpcomingProducts() {
        int count = productService.triggerUpcomingSeckillPreheat(advanceMinutes);
        if (count <= 0) {
            return;
        }
        log.info("自动预热秒杀库存完成, count={}, advanceMinutes={}", count, advanceMinutes);
    }
}
