package com.huixiang.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.UserQuery;
import com.huixiang.result.Result;
import com.huixiang.service.UserService;
import com.huixiang.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<Page<UserInfoVO>> page(UserQuery userQuery) {
        Page<UserInfoVO> pageResult = userService.page(userQuery);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<UserInfoVO> detail(@PathVariable Long id) {
        UserInfoVO userInfoVO = userService.detail(id);
        return Result.success(userInfoVO);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StatusUpdateDTO statusUpdateDTO) {
        Boolean result = userService.updateStatus(id, statusUpdateDTO);
        return Result.success(result);
    }
}