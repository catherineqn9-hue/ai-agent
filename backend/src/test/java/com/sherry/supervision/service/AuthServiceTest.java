package com.sherry.supervision.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sherry.supervision.dto.AuthLoginRequest;
import com.sherry.supervision.dto.AuthRegisterRequest;
import com.sherry.supervision.dto.AuthSessionResponse;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.exception.BusinessConflictException;
import com.sherry.supervision.exception.UnauthorizedException;
import com.sherry.supervision.mapper.AppUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthServiceTest {

    private AppUserMapper appUserMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        appUserMapper = mock(AppUserMapper.class);
        authService = new AuthService(appUserMapper);
    }

    @Test
    void shouldRegisterNewUserAndReturnSession() {
        when(appUserMapper.selectOne(any())).thenReturn(null);

        AuthSessionResponse session = authService.register(new AuthRegisterRequest(
                "AdminUser", "管理员", "password123"));

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.user().username()).isEqualTo("adminuser");
        assertThat(session.user().displayName()).isEqualTo("管理员");
        verify(appUserMapper).insert(any(AppUser.class));
        verify(appUserMapper).updateById(any(AppUser.class));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        when(appUserMapper.selectOne(any())).thenReturn(new AppUser());

        assertThatThrownBy(() -> authService.register(new AuthRegisterRequest(
                "admin", "管理员", "password123")))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void shouldLoginWithRegisteredPassword() {
        AppUser registeredUser = registerAndCaptureUser();
        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);

        AuthSessionResponse session = authService.login(new AuthLoginRequest("admin", "password123"));

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.user().username()).isEqualTo("admin");
    }

    @Test
    void shouldRejectInvalidPassword() {
        AppUser registeredUser = registerAndCaptureUser();
        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("admin", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("用户名或密码不正确");
    }

    @Test
    void shouldReturnCurrentUserByBearerToken() {
        AppUser registeredUser = registerAndCaptureUser();
        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);
        AuthSessionResponse session = authService.login(new AuthLoginRequest("admin", "password123"));

        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);

        assertThat(authService.currentUser("Bearer " + session.accessToken()).username()).isEqualTo("admin");
    }

    @Test
    void shouldClearSessionOnLogout() {
        AppUser registeredUser = registerAndCaptureUser();
        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);
        AuthSessionResponse session = authService.login(new AuthLoginRequest("admin", "password123"));

        when(appUserMapper.selectOne(any())).thenReturn(registeredUser);

        authService.logout("Bearer " + session.accessToken());

        verify(appUserMapper).update(any(), any());
    }

    private AppUser registerAndCaptureUser() {
        when(appUserMapper.selectOne(any())).thenReturn(null);
        authService.register(new AuthRegisterRequest("admin", "管理员", "password123"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserMapper).insert(captor.capture());
        return captor.getValue();
    }
}
