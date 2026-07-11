package com.huixiang.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /**
     * 手机号
     */
    private String phone;
    /**
     * 密码
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像地址
     */
    private String avatar;
    /**
     * 角色
     */
    private String role;
    /**
     * 账号状态
     */
    private Integer status;
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
}
