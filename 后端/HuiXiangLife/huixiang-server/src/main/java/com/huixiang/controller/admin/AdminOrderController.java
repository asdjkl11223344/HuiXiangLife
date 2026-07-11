package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.AdminOrderRefundDTO;
import com.huixiang.query.OrderQuery;
import com.huixiang.result.Result;
import com.huixiang.service.OrderService;
import com.huixiang.vo.OrderDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping("/page")
    public Result<Page<OrderDetailVO>> page(OrderQuery orderQuery) {
        Page<OrderDetailVO> pageResult = orderService.adminPage(orderQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        OrderDetailVO orderDetailVO = orderService.adminDetail(id);
        return Result.success(orderDetailVO);
    }

    @PutMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        Boolean result = orderService.adminCancel(id);
        return Result.success(result);
    }

    @PostMapping("/{id}/refund")
    public Result<Boolean> refund(@PathVariable Long id,
                                  @Valid @RequestBody AdminOrderRefundDTO refundDTO) {
        Boolean result = orderService.adminRefund(id, refundDTO);
        return Result.success(result);
    }
}