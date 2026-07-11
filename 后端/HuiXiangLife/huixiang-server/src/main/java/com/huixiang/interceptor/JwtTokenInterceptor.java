package com.huixiang.interceptor;

import com.huixiang.constant.SecurityConstant;
import com.huixiang.constant.UserConstant;
import com.huixiang.context.BaseContext;
import com.huixiang.exception.UnauthorizedException;
import com.huixiang.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(SecurityConstant.AUTHORIZATION_HEADER);

        if (!StringUtils.hasText(authorization) || !authorization.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            throw new UnauthorizedException("请先登录");
        }

        String token = authorization.substring(SecurityConstant.TOKEN_PREFIX.length());

        String blacklistKey = SecurityConstant.USER_TOKEN_BLACKLIST_PREFIX + token;
        if (stringRedisTemplate.hasKey(blacklistKey)) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }

        try {
            Claims claims = jwtUtil.parseUserToken(token);
            Long userId = Long.valueOf(claims.get("userId").toString());
            String role = String.valueOf(claims.get("role"));
            if (!UserConstant.ROLE_USER.equals(role)) {
                throw new UnauthorizedException("无用户端访问权限");
            }
            BaseContext.setCurrentId(userId);
            return true;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
