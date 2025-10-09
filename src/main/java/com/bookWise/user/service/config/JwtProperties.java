package com.bookWise.user.service.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
    private String secretKey;
    private long accessTokenExpirationMs = Duration.of(60, ChronoUnit.MINUTES).toMillis();
    private long refreshTokenExpirationMs = Duration.of(7, ChronoUnit.DAYS).toMillis();
    private long passwordResetTokenExpirationMs = Duration.of(1, ChronoUnit.HOURS).toMillis();
    private long emailVerificationTokenExpirationMs = Duration.of(24, ChronoUnit.HOURS).toMillis();
    private String tokenHeader = "Authorization";
    private String tokenPrefix = "Bearer ";
    private int maxDevicesPerUser = 5;

    @PostConstruct
    public void validate() {
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("A propriedade 'jwt.secret-key' não pode estar vazia");
        }

        if (secretKey.length() < 32) {
            log.warn("A chave secreta JWT deve ter pelo menos 32 caracteres para segurança adequada. Tamanho atual: {} caracteres.",
                    secretKey.length());
        }

        if (accessTokenExpirationMs < 60000) { // 1 minuto
            throw new IllegalStateException("O tempo de expiração do token de acesso deve ser de pelo menos 1 minuto");
        }

        if (refreshTokenExpirationMs <= accessTokenExpirationMs) {
            throw new IllegalStateException("O tempo de expiração do token de atualização deve ser maior que o do token de acesso");
        }

        log.info("Configurações JWT carregadas com sucesso. Expiração do token de acesso: {} ms, Expiração do token de atualização: {} ms",
                accessTokenExpirationMs, refreshTokenExpirationMs);
    }

    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpirationMs / 1000;
    }
}
