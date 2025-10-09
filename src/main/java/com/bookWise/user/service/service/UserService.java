package com.bookWise.user.service.service;

import com.bookWise.user.service.exception.ResourceAlreadyExistsException;
import com.bookWise.user.service.exception.ResourceNotFoundException;
import com.bookWise.user.service.mapper.UserEventMapper;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.model.enums.EventType;
import com.bookWise.user.service.publisher.UserEventPublisher;
import com.bookWise.user.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher userEventPublisher;
    private final UserEventMapper userEventMapper;

    @Transactional
    public Optional<User> findById(UUID id) {
        log.debug("Buscando usuário por ID: {}", id);
        return userRepository.findById(id);
    }

    @Transactional
    public User registerUser(User user) {
        log.debug("Iniciando registro do usuário: {}", user.getEmail());

        userRepository.findByEmailIgnoreCase(user.getEmail())
                .ifPresent(existingUser -> {
                    log.warn("Tentativa de registrar um e-mail já existente: {}", user.getEmail());
                    throw new ResourceAlreadyExistsException("Usuário com este e-mail já está cadastrado");
                });

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setActive(true);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        userEventPublisher.publish(userEventMapper.toUserEvent(savedUser, EventType.USER_CREATED));
        log.info("Usuário registrado com sucesso. ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional
    public User updateUser(UUID id, User userRequest) {
        log.debug("Iniciando atualização do usuário: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", id));

        if (userRepository.findByEmailIgnoreCase(user.getPassword()).isPresent()) {
            log.warn("Tentativa de atualizar uma senha já existente: {}", userRequest.getEmail());
            throw new ResourceAlreadyExistsException("Não é possível atualizar uma senha já existente");
        }

        user.setName(userRequest.getName());
        user.setEmail(userRequest.getEmail());
        user.setBio(userRequest.getBio());
        user.setAvatarUrl(userRequest.getAvatarUrl());
        user.setActive(userRequest.isActive());
        user.setEmailVerified(userRequest.isEmailVerified());
        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        userEventPublisher.publish(userEventMapper.toUserEvent(user, EventType.USER_UPDATED));

        return userRepository.save(user);
    }
}
