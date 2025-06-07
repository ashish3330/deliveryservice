package com.railswad.deliveryservice.util;

import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String ACCESS_TOKEN_NAME = "jwtToken";
    private static final String REFRESH_TOKEN_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Strict";
    private static final long ACCESS_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds
    private static final long REFRESH_TOKEN_MAX_AGE = 30 * 24 * 60 * 60; // 30 days in seconds
    private static final long CLEAR_COOKIE_MAX_AGE = 0;

    public ResponseCookie createAccessCookie(String accessToken) {
        return ResponseCookie.from(ACCESS_TOKEN_NAME, accessToken)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .sameSite(SAME_SITE)
                .build();
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .sameSite(SAME_SITE)
                .build();
    }

    public ResponseCookie clearAccessCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(CLEAR_COOKIE_MAX_AGE)
                .sameSite(SAME_SITE)
                .build();
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(CLEAR_COOKIE_MAX_AGE)
                .sameSite(SAME_SITE)
                .build();
    }

    public Cookie createServletAccessCookie(String accessToken) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_NAME, accessToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge((int) ACCESS_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", SAME_SITE);
        return cookie;
    }

    public Cookie createServletRefreshCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge((int) REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", SAME_SITE);
        return cookie;
    }
}