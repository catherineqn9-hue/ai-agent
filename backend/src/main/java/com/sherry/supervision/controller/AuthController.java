package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.AuthLoginRequest;
import com.sherry.supervision.dto.AuthRegisterRequest;
import com.sherry.supervision.dto.AuthSessionResponse;
import com.sherry.supervision.dto.AuthUserResponse;
import com.sherry.supervision.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthSessionResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthSessionResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(authService.currentUser(authorization));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Boolean>> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.ok(Map.of("logged_out", true));
    }
}
