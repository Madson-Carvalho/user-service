package com.bookWise.user.service.service;

import com.bookWise.user.service.exception.AuthenticationException;
import com.bookWise.user.service.exception.ResourceNotFoundException;
import com.bookWise.user.service.model.dto.AccessTokenDTO;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.repository.UserRepository;
import com.bookWise.user.service.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthService {
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccessTokenDTO login(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new AuthenticationException("Email and password are required");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Usuário com e-mail '{}' não encontrado", email);
                    return new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
                });

        try {
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            log.info("User {} logged in successfully", user.getEmail());
            return new AccessTokenDTO(accessToken, refreshToken);

        } catch (Exception e) {
            throw new AuthenticationException("Invalid email or password");
        }
    }

    @Transactional
    public AccessTokenDTO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthenticationException("Refresh token is required");
        }

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        UUID userId = UUID.fromString(tokenProvider.getUserIdFromJWT(refreshToken));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        log.info("Refreshed tokens for user {}", user.getEmail());
        return new AccessTokenDTO(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Usuário com e-mail '{}' não encontrado", email);
                    return new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
                });

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user {}", email);
    }
}
