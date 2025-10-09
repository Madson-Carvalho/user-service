package com.bookWise.user.service.controller;

import com.bookWise.user.service.model.dto.AccessTokenDTO;
import com.bookWise.user.service.model.dto.ChangePasswordRequest;
import com.bookWise.user.service.model.dto.LoginRequest;
import com.bookWise.user.service.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserAuthService userAuthService;

    @PostMapping("/login")
    public ResponseEntity<AccessTokenDTO> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        AccessTokenDTO tokens = userAuthService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenDTO> refreshToken(@RequestParam String refreshToken) {
        log.info("Refresh token request received");
        AccessTokenDTO tokens = userAuthService.refreshToken(refreshToken);
        return ResponseEntity.ok(tokens);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        log.info("Password change request for user: {}", email);
        userAuthService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            SecurityContextHolder.clearContext();
            log.info("User logged out: {}", authentication.getName());
        }
        return ResponseEntity.noContent().build();
    }
}
