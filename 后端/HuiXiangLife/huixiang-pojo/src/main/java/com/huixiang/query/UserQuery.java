package com.huixiang.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageQuery {

    /**
     * 关键字：手机号或昵称
     */
    private String keyword;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 账号状态
     */
    private Integer status;
}