package com.huixiang.utils;

import com.huixiang.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String createUserToken(Long userId, String role) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + jwtProperties.getUserTtl());

        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getUserSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(key)
                .compact();
    }

    public Claims parseUserToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getUserSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String createAdminToken(Long id, String role) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + jwtProperties.getAdminTtl());

        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getAdminSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claim("userId", id)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireTime)
                .signWith(key)
                .compact();
    }

    public Claims parseAdminToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getAdminSecretKey().getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
