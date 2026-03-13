package com.example.bankcards.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-token-expire-time}")
    private long accessTokenExpireTime;

    @Value("${security.jwt.refresh-token-expire-time}")
    private long refreshTokenExpireTime;

    public String getSecretKey() {
        return secretKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public long getAccessTokenExpireTime() {
        return accessTokenExpireTime;
    }

    public long getRefreshTokenExpireTime() {
        return refreshTokenExpireTime;
    }
}