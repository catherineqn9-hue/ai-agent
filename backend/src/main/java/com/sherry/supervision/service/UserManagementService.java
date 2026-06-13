package com.sherry.supervision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherry.supervision.dto.ManagedUserResponse;
import com.sherry.supervision.dto.UpdateManagedUserRequest;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.exception.ResourceNotFoundException;
import com.sherry.supervision.mapper.AppUserMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserManagementService {

    private final AppUserMapper appUserMapper;

    public UserManagementService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public List<ManagedUserResponse> listUsers() {
        return appUserMapper.selectList(new LambdaQueryWrapper<AppUser>()
                        .orderByAsc(AppUser::getDepartmentId, AppUser::getRoleKey, AppUser::getUsername))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ManagedUserResponse updateUser(UUID id, UpdateManagedUserRequest request) {
        AppUser user = appUserMapper.selectById(id);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在：" + id);
        }
        user.setDisplayName(request.displayName());
        user.setDepartmentId(request.departmentId());
        user.setDepartmentName(request.departmentName());
        user.setRoleKey(request.roleKey());
        user.setRoleName(request.roleName());
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setUpdatedAt(OffsetDateTime.now());
        appUserMapper.updateById(user);
        return toResponse(user);
    }

    public ManagedUserResponse toResponse(AppUser user) {
        return new ManagedUserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                user.getDepartmentName(),
                user.getRoleKey(),
                user.getRoleName(),
                user.getEnabled());
    }
}
