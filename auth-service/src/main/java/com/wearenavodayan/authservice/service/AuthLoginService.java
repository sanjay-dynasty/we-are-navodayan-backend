package com.wearenavodayan.authservice.service;

import java.util.Locale;

import com.wearenavodayan.authservice.dto.LoginRequest;
import com.wearenavodayan.authservice.dto.LoginResponse;
import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthLoginService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthLoginService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService) {

        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        UserAccount account = userAccountRepository
                .findByEmail(normalizedEmail)
                .filter(UserAccount::isEnabled)
                .filter(user -> passwordEncoder.matches(
                        request.password(), user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        String accessToken = jwtTokenService.createAccessToken(account);
        String refreshToken = refreshTokenService.issueRefreshToken(account);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenService.accessTokenLifetimeSeconds(),
                refreshToken,
                refreshTokenService.refreshTokenLifetimeSeconds(),
                new LoginResponse.UserDetails(
                        account.getId(),
                        account.getFullName(),
                        account.getEmail(),
                        account.getRole()));
    }
}