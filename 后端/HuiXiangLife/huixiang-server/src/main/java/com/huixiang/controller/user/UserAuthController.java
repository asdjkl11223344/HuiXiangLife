package com.huixiang.controller.user;

import com.huixiang.constant.SecurityConstant;
import com.huixiang.dto.UserLoginDTO;
import com.huixiang.dto.UserRegisterDTO;
import com.huixiang.result.Result;
import com.huixiang.service.AuthService;
import com.huixiang.vo.LoginVO;
import com.huixiang.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class UserAuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO){
        Long userId=authService.register(userRegisterDTO);
        return Result.success(userId);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody UserLoginDTO userLoginDTO){
        LoginVO loginVO=authService.login(userLoginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(){
        UserInfoVO userInfoVO=authService.me();
        return Result.success(userInfoVO);
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(@RequestHeader(SecurityConstant.AUTHORIZATION_HEADER) String authorization){
        return Result.success(authService.logout(authorization));
    }
}
