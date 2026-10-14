package com.wearenavodayan.authservice.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import com.wearenavodayan.authservice.dto.LoginResponse;
import com.wearenavodayan.authservice.entity.RefreshToken;
import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.repository.RefreshTokenRepository;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenService jwtTokenService;
    private final long refreshTokenDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            JwtTokenService jwtTokenService,
            @Value("${app.security.jwt.refresh-token-days:7}")
            long refreshTokenDays) {

        if (refreshTokenDays <= 0) {
            throw new IllegalArgumentException(
                    "Refresh token lifetime must be positive");
        }

        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public String issueRefreshToken(UserAccount account) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(account.getId());
        token.setTokenHash(hashToken(rawToken));
        token.setExpiresAt(
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));

        refreshTokenRepository.save(token);

        return rawToken;
    }

    @Transactional
    public LoginResponse refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = Instant.now();

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashForUpdate(hashToken(rawToken))
                .filter(token -> !token.isRevoked())
                .filter(token -> token.getExpiresAt().isAfter(now))
                .orElseThrow(InvalidRefreshTokenException::new);

        UserAccount account = userAccountRepository
                .findById(storedToken.getUserId())
                .filter(UserAccount::isEnabled)
                .orElseThrow(InvalidRefreshTokenException::new);

        // Revoke the old token before issuing its replacement.
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String replacementRefreshToken = issueRefreshToken(account);
        String accessToken = jwtTokenService.createAccessToken(account);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenService.accessTokenLifetimeSeconds(),
                replacementRefreshToken,
                refreshTokenLifetimeSeconds(),
                new LoginResponse.UserDetails(
                        account.getId(),
                        account.getFullName(),
                        account.getEmail(),
                        account.getRole()));
    }

    public long refreshTokenLifetimeSeconds() {
        return Math.multiplyExact(
                refreshTokenDays, 24L * 60L * 60L);
    }

    public static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable", exception);
        }
    }
}