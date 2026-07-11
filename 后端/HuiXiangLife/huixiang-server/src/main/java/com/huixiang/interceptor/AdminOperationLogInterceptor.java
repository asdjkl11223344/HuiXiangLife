package com.huixiang.interceptor;

import com.huixiang.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminOperationLogInterceptor implements HandlerInterceptor {

    private final OperationLogService operationLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        if (!shouldRecord(request, response, ex)) {
            return;
        }
        operationLogService.recordAdminOperation(request, handlerMethod);
    }

    private boolean shouldRecord(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        if (ex != null || response.getStatus() >= 400) {
            return false;
        }
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/admin/")
                && !uri.equals("/admin/auth/login");
    }
}
