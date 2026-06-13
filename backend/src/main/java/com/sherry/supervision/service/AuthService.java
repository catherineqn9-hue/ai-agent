package com.sherry.supervision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sherry.supervision.dto.AuthLoginRequest;
import com.sherry.supervision.dto.AuthRegisterRequest;
import com.sherry.supervision.dto.AuthSessionResponse;
import com.sherry.supervision.dto.AuthUserResponse;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.exception.BusinessConflictException;
import com.sherry.supervision.exception.InvalidRequestException;
import com.sherry.supervision.exception.UnauthorizedException;
import com.sherry.supervision.mapper.AppUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final int TOKEN_BYTES = 32;
    private static final int SESSION_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserMapper appUserMapper;

    public AuthService(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    public AuthSessionResponse register(AuthRegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (findByUsername(username) != null) {
            throw new BusinessConflictException("用户名已存在");
        }

        byte[] salt = randomBytes(16);
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setDisplayName(request.displayName().trim());
        user.setDepartmentId("operations");
        user.setDepartmentName("运营部");
        user.setRoleKey("member");
        user.setRoleName("成员");
        user.setPasswordSalt(encode(salt));
        user.setPasswordHash(hashPassword(request.password(), salt));
        user.setEnabled(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        appUserMapper.insert(user);
        return createSession(user);
    }

    public AuthSessionResponse login(AuthLoginRequest request) {
        AppUser user = findByUsername(normalizeUsername(request.username()));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new UnauthorizedException("用户名或密码不正确");
        }
        byte[] salt = decode(user.getPasswordSalt());
        String expectedHash = hashPassword(request.password(), salt);
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.UTF_8),
                user.getPasswordHash().getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("用户名或密码不正确");
        }
        return createSession(user);
    }

    public AuthUserResponse currentUser(String authorizationHeader) {
        AppUser user = requireUser(authorizationHeader);
        return toUserResponse(user);
    }

    public void logout(String authorizationHeader) {
        AppUser user = requireUser(authorizationHeader);
        appUserMapper.update(null, new UpdateWrapper<AppUser>()
                .eq("id", user.getId())
                .set("session_token_hash", null)
                .set("session_expires_at", null)
                .set("updated_at", OffsetDateTime.now()));
    }

    public AppUser requireUser(String authorizationHeader) {
        return findByBearerToken(authorizationHeader);
    }

    private AuthSessionResponse createSession(AppUser user) {
        String token = encode(randomBytes(TOKEN_BYTES));
        user.setSessionTokenHash(hashToken(token));
        user.setSessionExpiresAt(OffsetDateTime.now().plusDays(SESSION_DAYS));
        user.setUpdatedAt(OffsetDateTime.now());
        appUserMapper.updateById(user);
        return new AuthSessionResponse(token, "Bearer", toUserResponse(user));
    }

    private AppUser findByBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("未登录");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("未登录");
        }
        AppUser user = appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getSessionTokenHash, hashToken(token))
                .eq(AppUser::getEnabled, true));
        if (user == null || user.getSessionExpiresAt() == null || user.getSessionExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("登录已失效");
        }
        return user;
    }

    private AppUser findByUsername(String username) {
        return appUserMapper.selectOne(new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));
    }

    private AuthUserResponse toUserResponse(AppUser user) {
        return new AuthUserResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                user.getDepartmentName(),
                user.getRoleKey(),
                user.getRoleName());
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new InvalidRequestException("用户名不能为空");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String hashPassword(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return encode(factory.generateSecret(spec).getEncoded());
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希失败", exception);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return encode(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Token 哈希失败", exception);
        }
    }

    private byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
