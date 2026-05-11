package com.graduation.autograding.auth;

import com.graduation.autograding.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final AuthTokenService authTokenService;

    public AuthInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("缺少登录凭证，请先登录系统。");
        }

        request.setAttribute(CURRENT_USER_ATTRIBUTE, authTokenService.authenticate(token));
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        String legacyToken = request.getHeader("X-Auth-Token");
        if (legacyToken != null && !legacyToken.isBlank()) {
            return legacyToken.trim();
        }
        return null;
    }
}
