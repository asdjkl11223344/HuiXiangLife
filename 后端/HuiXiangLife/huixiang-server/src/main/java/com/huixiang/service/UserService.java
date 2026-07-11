package com.huixiang.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.query.UserQuery;
import com.huixiang.vo.UserInfoVO;

public interface UserService {

    Page<UserInfoVO> page(UserQuery userQuery);

    UserInfoVO detail(Long id);

    Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO);
}