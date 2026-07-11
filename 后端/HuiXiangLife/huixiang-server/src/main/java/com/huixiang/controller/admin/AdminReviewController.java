package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.query.ReviewQuery;
import com.huixiang.result.Result;
import com.huixiang.service.ReviewService;
import com.huixiang.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping("/page")
    public Result<Page<ReviewVO>> page(ReviewQuery reviewQuery) {
        Page<ReviewVO> pageResult = reviewService.adminPage(reviewQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<ReviewVO> detail(@PathVariable Long id) {
        ReviewVO reviewVO = reviewService.adminDetail(id);
        return Result.success(reviewVO);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @RequestParam Integer status) {
        Boolean result = reviewService.adminUpdateStatus(id, status);
        return Result.success(result);
    }
}