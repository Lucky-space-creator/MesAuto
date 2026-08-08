package com.lucky.mescore.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expire-seconds}") long expireSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(Long userId, String username, String tenantId, Map<String, Object> claims) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireSeconds * 1000));

        if (claims != null) {
            claims.forEach(builder::claim);
        }
        return builder.signWith(secretKey).compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token已过期");
            return null;
        } catch (Exception e) {
            log.debug("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims != null ? Long.valueOf(claims.getSubject()) : null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims == null;
    }
}
