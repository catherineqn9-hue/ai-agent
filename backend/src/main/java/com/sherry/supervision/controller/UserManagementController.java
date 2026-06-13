package com.sherry.supervision.controller;

import com.sherry.supervision.common.ApiResponse;
import com.sherry.supervision.dto.ManagedUserResponse;
import com.sherry.supervision.dto.UpdateManagedUserRequest;
import com.sherry.supervision.service.UserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<ManagedUserResponse>>> list() {
        return ApiResponse.ok(Map.of("users", userManagementService.listUsers()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ManagedUserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManagedUserRequest request) {
        return ApiResponse.ok(userManagementService.updateUser(id, request));
    }
}
