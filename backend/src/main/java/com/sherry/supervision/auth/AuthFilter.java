package com.sherry.supervision.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.common.BusinessCode;
import com.sherry.supervision.exception.UnauthorizedException;
import com.sherry.supervision.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = List.of("/", "/admin", "/index.html", "/health", "/favicon.ico");
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/assets/",
            "/api/v1/auth/",
            "/actuator/",
            "/swagger-ui",
            "/v3/api-docs");

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public AuthFilter(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE,
                    authService.requireUser(request.getHeader("Authorization")));
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            response.setStatus(BusinessCode.UNAUTHORIZED.httpStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail(BusinessCode.UNAUTHORIZED, exception.getMessage())));
        }
    }

    private boolean isPublicPath(String uri) {
        if (PUBLIC_PATHS.contains(uri)) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
    }
}
