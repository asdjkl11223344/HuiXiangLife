package com.huixiang.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.ReviewCreateDTO;
import com.huixiang.query.ReviewQuery;
import com.huixiang.result.Result;
import com.huixiang.service.ReviewService;
import com.huixiang.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/review")
@RequiredArgsConstructor
public class UserReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ReviewCreateDTO reviewCreateDTO){
        Long id = reviewService.create(reviewCreateDTO);
        return Result.success(id);
    }

    @GetMapping("/page")
    public Result<Page<ReviewVO>> page(ReviewQuery reviewQuery) {
        Page<ReviewVO> pageResult = reviewService.page(reviewQuery);
        return Result.success(pageResult);
    }
}