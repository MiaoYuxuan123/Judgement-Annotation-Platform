package edu.nju.jap.service.support;

import edu.nju.jap.config.JwtProperties;
import edu.nju.jap.mapper.SysUserMapper;
import edu.nju.jap.model.entity.User;
import edu.nju.jap.model.po.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationHours;
    private final SysUserMapper sysUserMapper;

    public JwtService(JwtProperties jwtProperties, SysUserMapper sysUserMapper) {
        byte[] secretBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("jap.jwt.secret 长度至少 32 字节");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationHours = jwtProperties.getExpirationHours();
        this.sysUserMapper = sysUserMapper;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expirationHours, ChronoUnit.HOURS);
        return Jwts.builder()
                .subject(String.valueOf(user.id))
                .claim("username", user.username)
                .claim("role", user.role)
                .claim("canCreateTask", user.canCreateTask)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public User resolveUser(String authorization) {
        String token = extractBearerToken(authorization);
        if (token == null) {
            return null;
        }
        Long userId = parseUserId(token);
        if (userId == null) {
            return null;
        }
        SysUser po = sysUserMapper.selectById(userId);
        if (po == null) {
            return null;
        }
        sysUserMapper.updateOnline(userId);
        return DomainConverter.toUser(po);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | NumberFormatException ex) {
            return null;
        }
    }
}
