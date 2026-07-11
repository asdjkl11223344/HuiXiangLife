package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.UserConstant;
import com.huixiang.dto.StatusUpdateDTO;
import com.huixiang.entity.SysUser;
import com.huixiang.exception.NotFoundException;
import com.huixiang.exception.ParameterException;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.query.UserQuery;
import com.huixiang.service.UserService;
import com.huixiang.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<UserInfoVO> page(UserQuery userQuery) {
        Page<SysUser> page = new Page<>(userQuery.getPageNo(), userQuery.getPageSize());
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getPhone, userQuery.getKeyword())
                    .or()
                    .like(SysUser::getNickname, userQuery.getKeyword()));
        }
        if (StringUtils.hasText(userQuery.getRole())) {
            queryWrapper.eq(SysUser::getRole, userQuery.getRole());
        }
        if (userQuery.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, userQuery.getStatus());
        }
        queryWrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> userPage = sysUserMapper.selectPage(page, queryWrapper);
        List<UserInfoVO> records = userPage.getRecords()
                .stream()
                .map(this::buildUserInfoVO)
                .toList();
        Page<UserInfoVO> resultPage = new Page<>(
                userPage.getCurrent(),
                userPage.getSize()
        );
        resultPage.setTotal(userPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public UserInfoVO detail(Long id) {
        String cacheKey = CacheConstant.ADMIN_USER_DETAIL_KEY_PREFIX + id;
        UserInfoVO cacheUserInfo = getUserDetailCache(cacheKey);
        if (cacheUserInfo != null) {
            return cacheUserInfo;
        }
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new NotFoundException("用户不存在");
        }
        UserInfoVO userInfoVO = buildUserInfoVO(sysUser);
        setUserDetailCache(cacheKey, userInfoVO);
        return userInfoVO;
    }

    @Override
    public Boolean updateStatus(Long id, StatusUpdateDTO statusUpdateDTO) {
        if (!UserConstant.STATUS_ENABLED.equals(statusUpdateDTO.getStatus())
                && !UserConstant.STATUS_DISABLED.equals(statusUpdateDTO.getStatus())) {
            throw new ParameterException("用户状态不正确");
        }
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new NotFoundException("用户不存在");
        }
        sysUser.setStatus(statusUpdateDTO.getStatus());
        sysUserMapper.updateById(sysUser);
        deleteAuthMeCache(id);
        deleteUserDetailCache(id);
        return true;
    }

    @SuppressWarnings("unchecked")
    private UserInfoVO getUserDetailCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof UserInfoVO userInfoVO) {
                return userInfoVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取管理端用户详情缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setUserDetailCache(String key, UserInfoVO userInfoVO) {
        try {
            long ttl = CacheConstant.ADMIN_USER_DETAIL_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.ADMIN_USER_DETAIL_TTL_RANDOM_BOUND_MINUTES + 1);
            redisTemplate.opsForValue().set(key, userInfoVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入管理端用户详情缓存失败, key={}", key, e);
        }
    }

    private void deleteAuthMeCache(Long userId) {
        try {
            redisTemplate.delete(CacheConstant.USER_ME_KEY_PREFIX + userId);
            redisTemplate.delete(CacheConstant.ADMIN_ME_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("删除当前登录信息缓存失败, userId={}", userId, e);
        }
    }

    private void deleteUserDetailCache(Long userId) {
        try {
            redisTemplate.delete(CacheConstant.ADMIN_USER_DETAIL_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("删除管理端用户详情缓存失败, userId={}", userId, e);
        }
    }

    private UserInfoVO buildUserInfoVO(SysUser sysUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(sysUser.getId());
        userInfoVO.setPhone(sysUser.getPhone());
        userInfoVO.setNickname(sysUser.getNickname());
        userInfoVO.setAvatar(sysUser.getAvatar());
        userInfoVO.setRole(sysUser.getRole());
        userInfoVO.setStatus(sysUser.getStatus());
        userInfoVO.setLastLoginTime(sysUser.getLastLoginTime());
        return userInfoVO;
    }
}
