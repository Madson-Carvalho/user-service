package com.bookWise.user.service.security;

import com.bookWise.user.service.exception.ResourceNotFoundException;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Tentando carregar usuário com e-mail: {}", email);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Usuário com e-mail '{}' não encontrado", email);
                    return new UsernameNotFoundException("Usuário não encontrado com o e-mail: " + email);
                });

        log.debug("Usuário carregado com sucesso: {}", email);
        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(String id) {
        log.debug("Carregando usuário por ID: {}", id);

        User user = userRepository.findById(UUID.fromString(id)).orElseThrow(
            () -> {
                log.error("Usuário não encontrado com ID: {}", id);
                return new ResourceNotFoundException("Usuário", "id", id);
            }
        );

        log.debug("Usuário carregado por ID com sucesso: {}", id);
        return UserPrincipal.create(user);
    }
}
