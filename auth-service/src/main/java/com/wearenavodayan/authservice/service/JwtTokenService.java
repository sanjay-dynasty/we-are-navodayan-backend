package com.wearenavodayan.authservice.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.wearenavodayan.authservice.entity.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final long accessTokenMinutes;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience,
            @Value("${app.security.jwt.access-token-minutes:15}")
            long accessTokenMinutes) {

        if (accessTokenMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Access token lifetime must be positive");
        }

        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String createAccessToken(UserAccount account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                accessTokenMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(account.getId().toString())
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", account.getRole())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

    public long accessTokenLifetimeSeconds() {
        return Math.multiplyExact(accessTokenMinutes, 60);
    }
}