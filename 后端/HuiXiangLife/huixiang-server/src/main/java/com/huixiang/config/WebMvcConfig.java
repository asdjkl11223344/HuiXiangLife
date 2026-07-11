package com.huixiang.config;

import com.huixiang.interceptor.AdminTokenInterceptor;
import com.huixiang.interceptor.AdminOperationLogInterceptor;
import com.huixiang.interceptor.JwtTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;
    private final AdminOperationLogInterceptor adminOperationLogInterceptor;
    private final JwtTokenInterceptor jwtTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns(
                        "/user/auth/logout",
                        "/user/auth/me",
                        "/user/coupon/receive",
                        "/user/coupon/my/**",
                        "/user/favorite",
                        "/user/favorite/**",
                        "/user/order/**",
                        "/user/review"
                );

        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");

        registry.addInterceptor(adminOperationLogInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/auth/login");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
