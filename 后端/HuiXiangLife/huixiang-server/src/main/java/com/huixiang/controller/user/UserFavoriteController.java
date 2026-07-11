package com.huixiang.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.FavoriteCreateDTO;
import com.huixiang.query.FavoriteQuery;
import com.huixiang.result.Result;
import com.huixiang.service.FavoriteService;
import com.huixiang.vo.FavoriteVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/favorite")
@RequiredArgsConstructor
public class UserFavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/page")
    public Result<Page<FavoriteVO>> page(FavoriteQuery favoriteQuery) {
        Page<FavoriteVO> page = favoriteService.page(favoriteQuery);
        return Result.success(page);
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody FavoriteCreateDTO favoriteCreateDTO) {
        Long id = favoriteService.create(favoriteCreateDTO);
        return Result.success(id);
    }

    @DeleteMapping
    public Result<Boolean> delete(@RequestParam Long targetId,
                                  @RequestParam Integer targetType) {
        Boolean result = favoriteService.delete(targetId,targetType);
        return Result.success(result);
    }
}