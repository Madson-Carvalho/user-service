package com.bookWise.user.service.controller;

import com.bookWise.user.service.model.dto.AccessTokenDTO;
import com.bookWise.user.service.model.dto.ChangePasswordRequest;
import com.bookWise.user.service.model.dto.LoginRequest;
import com.bookWise.user.service.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthControllerTest {

	@Mock
	private UserAuthService userAuthService;

	@InjectMocks
	private UserAuthController userAuthController;

	private static final String EMAIL = "test@example.com";
	private static final String PASSWORD = "password123";
	private static final String ACCESS_TOKEN = "test-access-token";
	private static final String REFRESH_TOKEN = "test-refresh-token";
	private static final String NEW_PASSWORD = "newPassword123";

	private void setupSecurityContext() {
		Authentication authentication = new UsernamePasswordAuthenticationToken(EMAIL, null);
		SecurityContext securityContext = mock(SecurityContext.class);
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void shouldReturnTokensWhenLoginWithValidCredentials() {
		LoginRequest loginRequest = new LoginRequest(EMAIL, PASSWORD);
		AccessTokenDTO expectedTokens = new AccessTokenDTO(ACCESS_TOKEN, REFRESH_TOKEN);

		when(userAuthService.login(EMAIL, PASSWORD)).thenReturn(expectedTokens);

		ResponseEntity<AccessTokenDTO> response = userAuthController.login(loginRequest);

		assertNotNull(response);
		assertEquals(200, response.getStatusCodeValue());
		assertNotNull(response.getBody());
		assertEquals(ACCESS_TOKEN, response.getBody().getAccessToken());
		assertEquals(REFRESH_TOKEN, response.getBody().getRefreshToken());

		verify(userAuthService).login(EMAIL, PASSWORD);
	}

	@Test
	void shouldReturnNewTokensWhenRefreshTokenIsValid() {
		AccessTokenDTO expectedTokens = new AccessTokenDTO("new-access-token", "new-refresh-token");
		when(userAuthService.refreshToken(REFRESH_TOKEN)).thenReturn(expectedTokens);

		ResponseEntity<AccessTokenDTO> response = userAuthController.refreshToken(REFRESH_TOKEN);

		assertNotNull(response);
		assertEquals(200, response.getStatusCodeValue());
		assertNotNull(response.getBody());
		assertEquals("new-access-token", response.getBody().getAccessToken());
		assertEquals("new-refresh-token", response.getBody().getRefreshToken());

		verify(userAuthService).refreshToken(REFRESH_TOKEN);
	}

	@Test
	void shouldReturnNoContentWhenChangePasswordWithValidRequest() {
		setupSecurityContext();
		ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);
		doNothing().when(userAuthService).changePassword(EMAIL, PASSWORD, NEW_PASSWORD);

		ResponseEntity<Void> response = userAuthController.changePassword(request);

		assertNotNull(response);
		assertEquals(204, response.getStatusCodeValue());
		assertNull(response.getBody());

		verify(userAuthService).changePassword(EMAIL, PASSWORD, NEW_PASSWORD);
	}

	@Test
	void shouldThrowExceptionWhenChangePasswordWithInvalidCurrentPassword() {
		setupSecurityContext();
		ChangePasswordRequest request = new ChangePasswordRequest("wrong-password", NEW_PASSWORD);
		doThrow(new RuntimeException("Senha atual inválida"))
				.when(userAuthService).changePassword(EMAIL, "wrong-password", NEW_PASSWORD);

		assertThrows(RuntimeException.class, () -> {
			userAuthController.changePassword(request);
		});

		verify(userAuthService).changePassword(EMAIL, "wrong-password", NEW_PASSWORD);
	}
}
