package com.lucky.mescore.common.util;

import org.apache.shiro.SecurityUtils;

public class SecurityUtil {

    private SecurityUtil() {}

    public static String getCurrentToken() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        return principal != null ? principal.toString() : null;
    }

    public static Long getCurrentUserId() {
        String token = getCurrentToken();
        if (token == null) return null;
        // JwtUtil instance is managed by Spring; we use token for lookup in realm
        return null;
    }
}
