package com.huixiang.controller.admin;

import com.huixiang.constant.SecurityConstant;
import com.huixiang.dto.UserLoginDTO;
import com.huixiang.result.Result;
import com.huixiang.service.AuthService;
import com.huixiang.vo.LoginVO;
import com.huixiang.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO userLoginDTO){
        LoginVO loginVO=authService.adminLogin(userLoginDTO);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(SecurityConstant.AUTHORIZATION_HEADER) String authorization) {
        return Result.success(authService.adminLogout(authorization));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.adminMe());
    }
}
