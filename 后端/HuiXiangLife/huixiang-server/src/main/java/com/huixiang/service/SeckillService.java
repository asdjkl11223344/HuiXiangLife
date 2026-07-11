package com.huixiang.service;

import com.huixiang.entity.Product;
import com.huixiang.vo.SeckillAdminStatusVO;
import com.huixiang.vo.SeckillResultVO;

public interface SeckillService {

    int PRE_DEDUCT_SUCCESS = 1;
    int PRE_DEDUCT_SOLD_OUT = 0;
    int PRE_DEDUCT_STOCK_NOT_INIT = -1;
    int PRE_DEDUCT_REPEAT = 2;

    int RESULT_CODE_EMPTY = 0;
    int RESULT_CODE_PENDING = 1;
    int RESULT_CODE_SUCCESS = 2;
    int RESULT_CODE_FAILED = 3;

    int FAILURE_CODE_REPEAT = 101;
    int FAILURE_CODE_SOLD_OUT = 102;
    int FAILURE_CODE_ACTIVITY_NOT_STARTED = 103;
    int FAILURE_CODE_ACTIVITY_ENDED = 104;
    int FAILURE_CODE_SYSTEM_BUSY = 199;

    String RESULT_EMPTY = "EMPTY";
    String RESULT_PENDING = "PENDING";
    String RESULT_SUCCESS = "SUCCESS";
    String RESULT_FAILED = "FAILED";

    long POLL_INTERVAL_PENDING_MILLIS = 1000L;
    long POLL_INTERVAL_IDLE_MILLIS = 1500L;

    int tryPreDeduct(Long userId, Product product, String requestId);

    void preheatStock(Product product);

    void resetStock(Product product);

    void markOrderCreated(Long orderId, Long userId, Long productId);

    void rollbackPreDeduct(Long userId, Long productId);

    void rollbackPreDeduct(Long userId, Long productId, Integer failureCode, String message);

    void markOrderFailed(Long userId, Long productId, Integer failureCode, String message);

    SeckillResultVO getResult(Long userId, Long productId);

    SeckillAdminStatusVO getAdminStatus(Long productId, Long userId);

    void restoreStockForCanceledOrder(Long orderId, Long productId);
}
