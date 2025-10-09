package com.bookWise.user.service.service;

import com.bookWise.user.service.exception.AuthenticationException;
import com.bookWise.user.service.model.dto.AccessTokenDTO;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.repository.UserRepository;
import com.bookWise.user.service.security.TokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TokenProvider tokenProvider;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserAuthService userAuthService;

	private User user;
	private final String email = "test@example.com";
	private final String password = "password123";
	private final String encodedPassword = "encodedPassword123";
	private final String accessToken = "access-token";
	private final String refreshToken = "refresh-token";

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setPassword(encodedPassword);
		user.setActive(true);
	}

	@Test
	void shouldReturnTokensWhenLoginWithValidCredentials() {
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
		when(tokenProvider.generateAccessToken(user)).thenReturn(accessToken);
		when(tokenProvider.generateRefreshToken(user)).thenReturn(refreshToken);

		AccessTokenDTO result = userAuthService.login(email, password);

		assertNotNull(result);
		assertEquals(accessToken, result.getAccessToken());
		assertEquals(refreshToken, result.getRefreshToken());
		verify(userRepository).findByEmailIgnoreCase(email);
		verify(tokenProvider).generateAccessToken(user);
		verify(tokenProvider).generateRefreshToken(user);
	}

	@Test
	void shouldThrowUsernameNotFoundExceptionWhenLoginWithInvalidEmail() {
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

		assertThrows(UsernameNotFoundException.class, () -> {
			userAuthService.login(email, password);
		});

		verify(userRepository).findByEmailIgnoreCase(email);
		verifyNoInteractions(passwordEncoder, tokenProvider);
	}

	@Test
	void shouldThrowAuthenticationExceptionWhenTokenGenerationFails() {
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
		when(tokenProvider.generateAccessToken(user)).thenThrow(new RuntimeException("Token generation failed"));

		assertThrows(AuthenticationException.class, () -> {
			userAuthService.login(email, password);
		});

		verify(userRepository).findByEmailIgnoreCase(email);
		verify(tokenProvider).generateAccessToken(user);
	}

	@Test
	void shouldUpdatePasswordWhenCurrentPasswordIsValid() {
		String newPassword = "newPassword123";
		String encodedNewPassword = "encodedNewPassword123";

		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
		when(passwordEncoder.matches(newPassword, encodedPassword)).thenReturn(false);
		when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		userAuthService.changePassword(email, password, newPassword);

		verify(userRepository).findByEmailIgnoreCase(email);
		verify(passwordEncoder).matches(password, encodedPassword);
		verify(passwordEncoder).matches(newPassword, encodedPassword);
		verify(passwordEncoder).encode(newPassword);
		verify(userRepository).save(any(User.class));
	}

	@Test
	void shouldReturnNewTokensWhenRefreshTokenIsValid() {
		UUID userId = user.getId();
		when(tokenProvider.validateToken(refreshToken)).thenReturn(true);
		when(tokenProvider.getUserIdFromJWT(refreshToken)).thenReturn(userId.toString());
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(tokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
		when(tokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");

		AccessTokenDTO result = userAuthService.refreshToken(refreshToken);

		assertNotNull(result);
		assertEquals("new-access-token", result.getAccessToken());
		assertEquals("new-refresh-token", result.getRefreshToken());

		verify(tokenProvider).validateToken(refreshToken);
		verify(tokenProvider).getUserIdFromJWT(refreshToken);
		verify(userRepository).findById(userId);
		verify(tokenProvider).generateAccessToken(user);
		verify(tokenProvider).generateRefreshToken(user);
	}
}
