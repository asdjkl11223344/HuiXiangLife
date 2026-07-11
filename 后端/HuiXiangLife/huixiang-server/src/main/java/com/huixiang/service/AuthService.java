package com.huixiang.service;

import com.huixiang.dto.UserLoginDTO;
import com.huixiang.dto.UserRegisterDTO;
import com.huixiang.vo.LoginVO;
import com.huixiang.vo.UserInfoVO;
import jakarta.validation.Valid;

public interface AuthService {

    Long register(UserRegisterDTO userRegisterDTO);

    LoginVO login(@Valid UserLoginDTO userLoginDTO);

    UserInfoVO me();

    Boolean logout(String authorization);

    LoginVO adminLogin(@Valid UserLoginDTO userLoginDTO);

    Boolean adminLogout(String authorization);

    UserInfoVO adminMe();
}
