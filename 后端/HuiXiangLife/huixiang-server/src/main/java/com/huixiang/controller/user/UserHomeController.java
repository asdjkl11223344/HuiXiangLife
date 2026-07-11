package com.huixiang.controller.user;

import com.huixiang.result.Result;
import com.huixiang.service.HomeService;
import com.huixiang.vo.HomeAggregateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/home")
@RequiredArgsConstructor
public class UserHomeController {

    private final HomeService homeService;

    @GetMapping
    public Result<HomeAggregateVO> aggregate() {
        HomeAggregateVO homeAggregateVO = homeService.aggregate();
        return Result.success(homeAggregateVO);
    }
}
