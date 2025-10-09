package com.bookWise.user.service.service;

import com.bookWise.user.service.exception.ResourceAlreadyExistsException;
import com.bookWise.user.service.exception.ResourceNotFoundException;
import com.bookWise.user.service.mapper.UserEventMapper;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.publisher.UserEventPublisher;
import com.bookWise.user.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private UserEventPublisher userEventPublisher;

	@Mock
	private UserEventMapper userEventMapper;

	@InjectMocks
	private UserService userService;

	@Captor
	private ArgumentCaptor<User> userCaptor;

	private User user;
	private final UUID userId = UUID.randomUUID();
	private final String email = "test@example.com";
	private final String password = "password123";
	private final String encodedPassword = "encodedPassword123";

	@BeforeEach
	void setUp() {
		user = new User();
		user.setId(userId);
		user.setName("Test User");
		user.setEmail(email);
		user.setPassword(password);
		user.setActive(true);
		user.setEmailVerified(false);
	}

	@Test
	void shouldReturnUserWhenFindByIdWithExistingId() {
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		Optional<User> result = userService.findById(userId);

		assertTrue(result.isPresent());
		assertEquals(userId, result.get().getId());
		verify(userRepository).findById(userId);
	}

	@Test
	void shouldReturnEmptyWhenFindByIdWithNonExistingId() {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		Optional<User> result = userService.findById(userId);

		assertFalse(result.isPresent());
		verify(userRepository).findById(userId);
	}

	@Test
	void shouldSaveUserWhenRegisterWithNewEmail() {
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
		when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
		when(userRepository.save(any(User.class))).thenReturn(user);

		User savedUser = userService.registerUser(user);

		assertNotNull(savedUser);
		assertEquals(userId, savedUser.getId());
		assertTrue(savedUser.isActive());
		assertFalse(savedUser.isEmailVerified());

		verify(userRepository).findByEmailIgnoreCase(email);
		verify(passwordEncoder).encode(password);
		verify(userRepository).save(userCaptor.capture());
		verify(userEventPublisher).publish(any());

		User capturedUser = userCaptor.getValue();
		assertEquals(encodedPassword, capturedUser.getPassword());
	}

	@Test
	void shouldThrowExceptionWhenRegisterWithExistingEmail() {
		when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

		assertThrows(ResourceAlreadyExistsException.class, () -> {
			userService.registerUser(user);
		});

		verify(userRepository).findByEmailIgnoreCase(email);
		verifyNoInteractions(passwordEncoder, userEventPublisher);
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldUpdateUserWhenUpdateWithValidData() {
		User updatedUser = new User();
		updatedUser.setName("Updated Name");
		updatedUser.setEmail("updated@example.com");
		updatedUser.setBio("New bio");
		updatedUser.setAvatarUrl("http://example.com/avatar.jpg");
		updatedUser.setActive(false);
		updatedUser.setEmailVerified(true);
		updatedUser.setPassword("newpassword");

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = userService.updateUser(userId, updatedUser);

		assertNotNull(result);
		assertEquals("Updated Name", result.getName());
		assertEquals("updated@example.com", result.getEmail());
		assertEquals("New bio", result.getBio());
		assertEquals("http://example.com/avatar.jpg", result.getAvatarUrl());
		assertFalse(result.isActive());
		assertTrue(result.isEmailVerified());

		verify(userRepository).findById(userId);
		verify(passwordEncoder).encode("newpassword");
		verify(userRepository).save(userCaptor.capture());
		verify(userEventPublisher).publish(any());

		User capturedUser = userCaptor.getValue();
		assertEquals("newEncodedPassword", capturedUser.getPassword());
	}

	@Test
	void shouldThrowExceptionWhenUpdateWithNonExistingId() {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> {
			userService.updateUser(userId, user);
		});

		verify(userRepository).findById(userId);
		verifyNoInteractions(passwordEncoder, userEventPublisher);
		verify(userRepository, never()).save(any());
	}
}
