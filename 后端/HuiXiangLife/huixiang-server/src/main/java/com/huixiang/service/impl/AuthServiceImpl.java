package com.huixiang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huixiang.constant.CacheConstant;
import com.huixiang.constant.SecurityConstant;
import com.huixiang.constant.UserConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.dto.UserLoginDTO;
import com.huixiang.dto.UserRegisterDTO;
import com.huixiang.entity.SysUser;
import com.huixiang.exception.BusinessException;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.mapper.SysUserMapper;
import com.huixiang.properties.JwtProperties;
import com.huixiang.service.AuthService;
import com.huixiang.utils.JwtUtil;
import com.huixiang.vo.LoginVO;
import com.huixiang.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Long register(UserRegisterDTO userRegisterDTO) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getPhone,userRegisterDTO.getPhone());
        SysUser existUser = sysUserMapper.selectOne(queryWrapper);
        if (existUser!=null){
            throw new BusinessException("手机号已注册");
        }
        SysUser sysUser = new SysUser();
        sysUser.setPhone(userRegisterDTO.getPhone());
        String encodedPassword = passwordEncoder.encode(userRegisterDTO.getPassword());
        sysUser.setPassword(encodedPassword);
        sysUser.setNickname(userRegisterDTO.getNickname());
        sysUser.setAvatar(userRegisterDTO.getAvatar());
        sysUser.setRole(UserConstant.ROLE_USER);
        sysUser.setStatus(UserConstant.STATUS_ENABLED);
        int rows = sysUserMapper.insert(sysUser);
        if (rows<=0){
            throw new BusinessException("注册失败");
        }
        return sysUser.getId();
    }

    @Override
    public LoginVO login(UserLoginDTO userLoginDTO) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getPhone,userLoginDTO.getPhone());
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser == null || !passwordEncoder.matches(userLoginDTO.getPassword(), sysUser.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (UserConstant.STATUS_DISABLED.equals(sysUser.getStatus())){
            throw new BusinessException("账号已被禁用");
        }
        sysUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(sysUser);
        deleteAuthMeCache(sysUser.getId());
        String token = jwtUtil.createUserToken(sysUser.getId(), sysUser.getRole());
        UserInfoVO userInfoVO = buildUserInfoVO(sysUser);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType(SecurityConstant.TOKEN_TYPE);
        loginVO.setExpireIn(jwtProperties.getUserTtl());
        loginVO.setUserInfo(userInfoVO);
        return loginVO;
    }

    @Override
    public UserInfoVO me() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId==null){
            throw new UnauthorizedException("请先登录");
        }
        String cacheKey = CacheConstant.USER_ME_KEY_PREFIX + currentId;
        UserInfoVO cacheUserInfo = getAuthMeCache(cacheKey);
        if (cacheUserInfo != null) {
            return cacheUserInfo;
        }
        SysUser sysUser = sysUserMapper.selectById(currentId);
        if (sysUser==null){
            throw new UnauthorizedException("用户不存在或登录已失效");
        }
        UserInfoVO userInfoVO = buildUserInfoVO(sysUser);
        setAuthMeCache(cacheKey, userInfoVO);
        return userInfoVO;
    }

    @Override
    public Boolean logout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            throw new UnauthorizedException("请先登录");
        }
        String token = authorization.substring(SecurityConstant.TOKEN_PREFIX.length());
        Claims claims;
        try {
            claims = jwtUtil.parseUserToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
        Date expiration = claims.getExpiration();
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            String blacklistKey = SecurityConstant.USER_TOKEN_BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue().set(blacklistKey, "1", ttlMillis, TimeUnit.MILLISECONDS);
        }
        deleteAuthMeCache(Long.valueOf(claims.get("userId").toString()));
        return true;
    }

    @Override
    public LoginVO adminLogin(UserLoginDTO userLoginDTO) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getPhone,userLoginDTO.getPhone());
        SysUser sysUser = sysUserMapper.selectOne(queryWrapper);
        if (sysUser==null||!passwordEncoder.matches(userLoginDTO.getPassword(),sysUser.getPassword())){
            throw new BusinessException("手机号或密码错误");
        }
        if (UserConstant.STATUS_DISABLED.equals(sysUser.getStatus())){
            throw new BusinessException("账号已被禁用");
        }
        if (!UserConstant.ROLE_ADMIN.equals(sysUser.getRole())) {
            throw new UnauthorizedException("无管理端访问权限");
        }
        sysUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(sysUser);
        deleteAuthMeCache(sysUser.getId());
        String token = jwtUtil.createAdminToken(sysUser.getId(), sysUser.getRole());
        UserInfoVO userInfoVO = buildUserInfoVO(sysUser);
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType(SecurityConstant.TOKEN_TYPE);
        loginVO.setExpireIn(jwtProperties.getAdminTtl());
        loginVO.setUserInfo(userInfoVO);
        return loginVO;
    }

    @Override
    public Boolean adminLogout(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            throw new UnauthorizedException("请先登录");
        }
        String token = authorization.substring(SecurityConstant.TOKEN_PREFIX.length());
        Claims claims;
        try {
            claims = jwtUtil.parseAdminToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
        Date expiration = claims.getExpiration();
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            String blacklistKey = SecurityConstant.ADMIN_TOKEN_BLACKLIST_PREFIX + token;
            stringRedisTemplate.opsForValue().set(blacklistKey, "1", ttlMillis, TimeUnit.MILLISECONDS);
        }
        deleteAuthMeCache(Long.valueOf(claims.get("userId").toString()));
        return true;
    }

    @Override
    public UserInfoVO adminMe() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new UnauthorizedException("请先登录");
        }
        String cacheKey = CacheConstant.ADMIN_ME_KEY_PREFIX + currentId;
        UserInfoVO cacheUserInfo = getAuthMeCache(cacheKey);
        if (cacheUserInfo != null) {
            return cacheUserInfo;
        }
        SysUser sysUser = sysUserMapper.selectById(currentId);
        if (sysUser == null) {
            throw new UnauthorizedException("用户不存在或登录已失效");
        }
        if (!UserConstant.ROLE_ADMIN.equals(sysUser.getRole())) {
            throw new UnauthorizedException("无管理端访问权限");
        }
        UserInfoVO userInfoVO = buildUserInfoVO(sysUser);
        setAuthMeCache(cacheKey, userInfoVO);
        return userInfoVO;
    }

    @SuppressWarnings("unchecked")
    private UserInfoVO getAuthMeCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof UserInfoVO userInfoVO) {
                return userInfoVO;
            }
            return null;
        } catch (Exception e) {
            log.warn("读取当前登录信息缓存失败, key={}", key, e);
            return null;
        }
    }

    private void setAuthMeCache(String key, UserInfoVO userInfoVO) {
        try {
            long ttl = CacheConstant.AUTH_ME_TTL_MINUTES
                    + ThreadLocalRandom.current().nextInt(CacheConstant.AUTH_ME_TTL_RANDOM_BOUND_MINUTES + 1);
            redisTemplate.opsForValue().set(key, userInfoVO, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入当前登录信息缓存失败, key={}", key, e);
        }
    }

    public void deleteAuthMeCache(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(CacheConstant.USER_ME_KEY_PREFIX + userId);
            redisTemplate.delete(CacheConstant.ADMIN_ME_KEY_PREFIX + userId);
            redisTemplate.delete(CacheConstant.ADMIN_USER_DETAIL_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("删除当前登录信息缓存失败, userId={}", userId, e);
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
