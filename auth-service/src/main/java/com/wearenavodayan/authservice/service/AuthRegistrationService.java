package com.wearenavodayan.authservice.service;

import com.wearenavodayan.authservice.dto.request.RegistrationRequest;
import com.wearenavodayan.authservice.dto.response.RegisteredUserResponse;
import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.exception.DuplicateEmailException;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthRegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthRegistrationService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisteredUserResponse register(RegistrationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setFullName(request.fullName().trim());
        account.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            UserAccount saved = userAccountRepository.saveAndFlush(account);

            return new RegisteredUserResponse(
                saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getCreatedAt()
            );
        } catch (DataIntegrityViolationException exception) {
            // Also handle a concurrent registration for the same email.
            throw new DuplicateEmailException();
        }
    }
}