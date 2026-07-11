package com.huixiang.service.impl;

import com.huixiang.constant.CacheConstant;
import com.huixiang.entity.Product;
import com.huixiang.service.SeckillService;
import com.huixiang.vo.SeckillAdminStatusVO;
import com.huixiang.vo.SeckillResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String SECKILL_PRE_DEDUCT_SCRIPT = """
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 2
            end
            local stock = redis.call('GET', KEYS[1])
            if not stock then
                return -1
            end
            stock = tonumber(stock)
            if stock <= 0 then
                return 0
            end
            redis.call('DECR', KEYS[1])
            redis.call('SET', KEYS[2], ARGV[1])
            redis.call('PEXPIRE', KEYS[2], ARGV[2])
            return 1
            """;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public int tryPreDeduct(Long userId, Product product, String requestId) {
        initStockIfAbsent(product);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SECKILL_PRE_DEDUCT_SCRIPT);
        script.setResultType(Long.class);
        String stockKey = buildSeckillStockKey(product.getId());
        String userOrderKey = buildSeckillUserOrderKey(userId, product.getId());
        String resultKey = buildSeckillResultKey(userId, product.getId());
        Long result = stringRedisTemplate.execute(
                script,
                List.of(stockKey, userOrderKey),
                requestId,
                String.valueOf(TimeUnit.HOURS.toMillis(CacheConstant.SECKILL_USER_ORDER_TTL_HOURS))
        );
        if (result != null && result.intValue() == PRE_DEDUCT_SUCCESS) {
            stringRedisTemplate.delete(resultKey);
        }
        return result == null ? PRE_DEDUCT_STOCK_NOT_INIT : result.intValue();
    }

    @Override
    public void preheatStock(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        String stockKey = buildSeckillStockKey(product.getId());
        long ttlHours = resolveStockTtlHours(product.getEndTime());
        int stock = product.getStock() == null ? 0 : Math.max(product.getStock(), 0);
        try {
            stringRedisTemplate.opsForValue().set(
                    stockKey,
                    String.valueOf(stock),
                    ttlHours,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("预热秒杀库存失败, productId={}", product.getId(), e);
        }
    }

    @Override
    public void resetStock(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        Long productId = product.getId();
        String stockKey = buildSeckillStockKey(productId);
        String userOrderPattern = CacheConstant.SECKILL_USER_ORDER_KEY_PREFIX + "*:" + productId;
        String resultPattern = CacheConstant.SECKILL_RESULT_KEY_PREFIX + "*:" + productId;
        String orderMappingPattern = CacheConstant.SECKILL_ORDER_MAPPING_KEY_PREFIX + "*";
        try {
            deleteKeysByPattern(userOrderPattern);
            deleteKeysByPattern(resultPattern);
            deleteOrderMappingKeysByProductId(orderMappingPattern, productId);
            long ttlHours = resolveStockTtlHours(product.getEndTime());
            int stock = product.getStock() == null ? 0 : Math.max(product.getStock(), 0);
            stringRedisTemplate.opsForValue().set(
                    stockKey,
                    String.valueOf(stock),
                    ttlHours,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("重置秒杀库存状态失败, productId={}", productId, e);
        }
    }

    @Override
    public void markOrderCreated(Long orderId, Long userId, Long productId) {
        if (orderId == null || userId == null || productId == null) {
            return;
        }
        String userOrderKey = buildSeckillUserOrderKey(userId, productId);
        String orderMappingKey = buildSeckillOrderMappingKey(orderId);
        String resultKey = buildSeckillResultKey(userId, productId);
        try {
            stringRedisTemplate.opsForValue().set(
                    userOrderKey,
                    String.valueOf(orderId),
                    CacheConstant.SECKILL_USER_ORDER_TTL_HOURS,
                    TimeUnit.HOURS
            );
            stringRedisTemplate.opsForValue().set(
                    orderMappingKey,
                    userId + ":" + productId,
                    CacheConstant.SECKILL_ORDER_MAPPING_TTL_HOURS,
                    TimeUnit.HOURS
            );
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    RESULT_SUCCESS + ":" + orderId,
                    CacheConstant.SECKILL_RESULT_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("标记秒杀订单成功状态失败, orderId={}, userId={}, productId={}", orderId, userId, productId, e);
        }
    }

    @Override
    public void rollbackPreDeduct(Long userId, Long productId) {
        rollbackPreDeduct(userId, productId, FAILURE_CODE_SYSTEM_BUSY, "抢购失败，请稍后重试");
    }

    @Override
    public void rollbackPreDeduct(Long userId, Long productId, Integer failureCode, String message) {
        rollbackPreDeductInternal(userId, productId, failureCode, message);
    }

    @Override
    public void markOrderFailed(Long userId, Long productId, Integer failureCode, String message) {
        if (userId == null || productId == null) {
            return;
        }
        String resultKey = buildSeckillResultKey(userId, productId);
        try {
            stringRedisTemplate.opsForValue().set(
                    resultKey,
                    buildFailedResultValue(failureCode, message),
                    CacheConstant.SECKILL_RESULT_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("标记秒杀失败结果失败, userId={}, productId={}", userId, productId, e);
        }
    }

    @Override
    public SeckillResultVO getResult(Long userId, Long productId) {
        if (userId == null || productId == null) {
            return buildEmptyResult();
        }
        String userOrderKey = buildSeckillUserOrderKey(userId, productId);
        String resultKey = buildSeckillResultKey(userId, productId);
        try {
            String resultValue = stringRedisTemplate.opsForValue().get(resultKey);
            if (resultValue != null && resultValue.startsWith(RESULT_SUCCESS + ":")) {
                String orderId = resultValue.substring((RESULT_SUCCESS + ":").length());
                if (isNumeric(orderId)) {
                    return buildSuccessResult(Long.valueOf(orderId));
                }
                return buildSuccessResult(null);
            }
            if (resultValue != null && resultValue.startsWith(RESULT_FAILED + ":")) {
                return buildFailedResult(parseFailureCode(resultValue), parseFailureMessage(resultValue));
            }
            String userOrderValue = stringRedisTemplate.opsForValue().get(userOrderKey);
            if (userOrderValue != null) {
                return buildPendingResult(userOrderValue);
            }
        } catch (Exception e) {
            log.warn("查询秒杀结果失败, userId={}, productId={}", userId, productId, e);
            return buildPendingResult(null);
        }
        return buildEmptyResult();
    }

    @Override
    public SeckillAdminStatusVO getAdminStatus(Long productId, Long userId) {
        SeckillAdminStatusVO seckillAdminStatusVO = new SeckillAdminStatusVO();
        seckillAdminStatusVO.setProductId(productId);
        seckillAdminStatusVO.setStockKey(buildSeckillStockKey(productId));
        try {
            String stockValue = stringRedisTemplate.opsForValue().get(seckillAdminStatusVO.getStockKey());
            seckillAdminStatusVO.setStockPreheated(hasText(stockValue));
            if (isNumeric(stockValue)) {
                seckillAdminStatusVO.setRedisStock(Integer.valueOf(stockValue));
            }
            seckillAdminStatusVO.setRelatedOrderMappingCount(countOrderMappingByProductId(productId));
            if (userId != null) {
                seckillAdminStatusVO.setUserId(userId);
                String userOrderKey = buildSeckillUserOrderKey(userId, productId);
                String resultKey = buildSeckillResultKey(userId, productId);
                seckillAdminStatusVO.setUserOrderKey(userOrderKey);
                seckillAdminStatusVO.setUserOrderValue(stringRedisTemplate.opsForValue().get(userOrderKey));
                seckillAdminStatusVO.setResultKey(resultKey);
                seckillAdminStatusVO.setResultValue(stringRedisTemplate.opsForValue().get(resultKey));
                seckillAdminStatusVO.setResult(getResult(userId, productId));
            }
        } catch (Exception e) {
            log.warn("查询管理端秒杀调试状态失败, productId={}, userId={}", productId, userId, e);
        }
        return seckillAdminStatusVO;
    }

    private void rollbackPreDeductInternal(Long userId, Long productId, Integer failureCode, String message) {
        if (userId == null || productId == null) {
            return;
        }
        String stockKey = buildSeckillStockKey(productId);
        String userOrderKey = buildSeckillUserOrderKey(userId, productId);
        try {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(userOrderKey);
            markOrderFailed(userId, productId, failureCode, message);
        } catch (Exception e) {
            log.warn("回滚秒杀预扣库存失败, userId={}, productId={}", userId, productId, e);
        }
    }

    @Override
    public void restoreStockForCanceledOrder(Long orderId, Long productId) {
        if (orderId == null || productId == null) {
            return;
        }
        String orderMappingKey = buildSeckillOrderMappingKey(orderId);
        try {
            String mappingValue = stringRedisTemplate.opsForValue().get(orderMappingKey);
            if (mappingValue == null) {
                return;
            }
            String[] parts = mappingValue.split(":");
            if (parts.length != 2) {
                stringRedisTemplate.delete(orderMappingKey);
                return;
            }
            String userOrderKey = buildSeckillUserOrderKey(Long.valueOf(parts[0]), productId);
            String stockKey = buildSeckillStockKey(productId);
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(List.of(orderMappingKey, userOrderKey));
        } catch (Exception e) {
            log.warn("恢复秒杀库存失败, orderId={}, productId={}", orderId, productId, e);
        }
    }

    private void initStockIfAbsent(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        String stockKey = buildSeckillStockKey(product.getId());
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                return;
            }
            long ttlHours = resolveStockTtlHours(product.getEndTime());
            stringRedisTemplate.opsForValue().setIfAbsent(
                    stockKey,
                    String.valueOf(product.getStock() == null ? 0 : product.getStock()),
                    ttlHours,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("初始化秒杀库存缓存失败, productId={}", product.getId(), e);
        }
    }

    private long resolveStockTtlHours(LocalDateTime endTime) {
        if (endTime == null) {
            return CacheConstant.SECKILL_ORDER_MAPPING_TTL_HOURS;
        }
        long hours = Duration.between(LocalDateTime.now(), endTime.plusHours(1)).toHours();
        return Math.max(hours, CacheConstant.SECKILL_ORDER_MAPPING_TTL_HOURS);
    }

    private String buildSeckillStockKey(Long productId) {
        return CacheConstant.SECKILL_STOCK_KEY_PREFIX + productId;
    }

    private String buildSeckillUserOrderKey(Long userId, Long productId) {
        return CacheConstant.SECKILL_USER_ORDER_KEY_PREFIX + userId + ":" + productId;
    }

    private String buildSeckillOrderMappingKey(Long orderId) {
        return CacheConstant.SECKILL_ORDER_MAPPING_KEY_PREFIX + orderId;
    }

    private String buildSeckillResultKey(Long userId, Long productId) {
        return CacheConstant.SECKILL_RESULT_KEY_PREFIX + userId + ":" + productId;
    }

    private void deleteKeysByPattern(String pattern) {
        List<String> keys = getKeys(pattern);
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private void deleteOrderMappingKeysByProductId(String pattern, Long productId) {
        List<String> keys = getKeys(pattern);
        if (keys.isEmpty()) {
            return;
        }
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.isEmpty()) {
            return;
        }
        java.util.ArrayList<String> matchedKeys = new java.util.ArrayList<>();
        String suffix = ":" + productId;
        for (int i = 0; i < keys.size() && i < values.size(); i++) {
            String value = values.get(i);
            if (value != null && value.endsWith(suffix)) {
                matchedKeys.add(keys.get(i));
            }
        }
        if (!matchedKeys.isEmpty()) {
            stringRedisTemplate.delete(matchedKeys);
        }
    }

    private List<String> getKeys(String pattern) {
        java.util.Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return List.copyOf(keys);
    }

    private int countOrderMappingByProductId(Long productId) {
        List<String> keys = getKeys(CacheConstant.SECKILL_ORDER_MAPPING_KEY_PREFIX + "*");
        if (keys.isEmpty()) {
            return 0;
        }
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.isEmpty()) {
            return 0;
        }
        int count = 0;
        String suffix = ":" + productId;
        for (String value : values) {
            if (value != null && value.endsWith(suffix)) {
                count++;
            }
        }
        return count;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private SeckillResultVO buildEmptyResult() {
        SeckillResultVO seckillResultVO = new SeckillResultVO();
        seckillResultVO.setStatusCode(RESULT_CODE_EMPTY);
        seckillResultVO.setStatus(RESULT_EMPTY);
        seckillResultVO.setFinished(true);
        seckillResultVO.setNextPollIntervalMillis(POLL_INTERVAL_IDLE_MILLIS);
        seckillResultVO.setMessage("暂无秒杀记录");
        return seckillResultVO;
    }

    private SeckillResultVO buildPendingResult(String requestId) {
        SeckillResultVO seckillResultVO = new SeckillResultVO();
        seckillResultVO.setStatusCode(RESULT_CODE_PENDING);
        seckillResultVO.setStatus(RESULT_PENDING);
        seckillResultVO.setFinished(false);
        seckillResultVO.setNextPollIntervalMillis(POLL_INTERVAL_PENDING_MILLIS);
        seckillResultVO.setRequestId(requestId);
        seckillResultVO.setMessage("正在排队创建订单");
        return seckillResultVO;
    }

    private SeckillResultVO buildSuccessResult(Long orderId) {
        SeckillResultVO seckillResultVO = new SeckillResultVO();
        seckillResultVO.setStatusCode(RESULT_CODE_SUCCESS);
        seckillResultVO.setStatus(RESULT_SUCCESS);
        seckillResultVO.setFinished(true);
        seckillResultVO.setNextPollIntervalMillis(0L);
        seckillResultVO.setOrderId(orderId);
        seckillResultVO.setMessage("秒杀成功，请尽快支付");
        return seckillResultVO;
    }

    private SeckillResultVO buildFailedResult(Integer failureCode, String message) {
        SeckillResultVO seckillResultVO = new SeckillResultVO();
        seckillResultVO.setStatusCode(RESULT_CODE_FAILED);
        seckillResultVO.setStatus(RESULT_FAILED);
        seckillResultVO.setFinished(true);
        seckillResultVO.setNextPollIntervalMillis(0L);
        seckillResultVO.setFailureCode(failureCode);
        seckillResultVO.setMessage(message == null || message.isBlank() ? "抢购失败，请稍后重试" : message);
        return seckillResultVO;
    }

    private String buildFailedResultValue(Integer failureCode, String message) {
        int code = failureCode == null ? FAILURE_CODE_SYSTEM_BUSY : failureCode;
        String resultMessage = message == null || message.isBlank() ? "抢购失败，请稍后重试" : message;
        return RESULT_FAILED + ":" + code + ":" + resultMessage;
    }

    private Integer parseFailureCode(String resultValue) {
        String[] parts = splitFailedResultValue(resultValue);
        if (parts.length < 3 || !isNumeric(parts[1])) {
            return FAILURE_CODE_SYSTEM_BUSY;
        }
        return Integer.valueOf(parts[1]);
    }

    private String parseFailureMessage(String resultValue) {
        String[] parts = splitFailedResultValue(resultValue);
        if (parts.length < 3) {
            return "抢购失败，请稍后重试";
        }
        return parts[2];
    }

    private String[] splitFailedResultValue(String resultValue) {
        return resultValue == null ? new String[0] : resultValue.split(":", 3);
    }
}
