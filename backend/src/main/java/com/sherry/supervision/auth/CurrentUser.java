package com.sherry.supervision.auth;

import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public final class CurrentUser {

    public static final String REQUEST_ATTRIBUTE = "currentUser";

    private CurrentUser() {
    }

    public static AppUser from(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof AppUser user) {
            return user;
        }
        throw new UnauthorizedException("未登录");
    }
}
