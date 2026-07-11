package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.query.OperationLogQuery;
import com.huixiang.result.Result;
import com.huixiang.service.OperationLogService;
import com.huixiang.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/operation-log")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<Page<OperationLogVO>> page(OperationLogQuery operationLogQuery) {
        return Result.success(operationLogService.page(operationLogQuery));
    }
}
