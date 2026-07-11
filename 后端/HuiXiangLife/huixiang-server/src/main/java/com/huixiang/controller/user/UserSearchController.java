package com.huixiang.controller.user;

import com.huixiang.result.Result;
import com.huixiang.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/search")
@RequiredArgsConstructor
public class UserSearchController {

    private final SearchService searchService;

    @GetMapping("/hot")
    public Result<List<String>> hot() {
        List<String> list = searchService.hotKeywords();
        return Result.success(list);
    }
}