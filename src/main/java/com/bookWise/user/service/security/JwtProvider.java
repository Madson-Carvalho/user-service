package com.bookWise.user.service.security;

import com.bookWise.user.service.config.JwtProperties;
import com.bookWise.user.service.model.entity.User;
import com.bookWise.user.service.model.entity.UserToken;
import com.bookWise.user.service.model.enums.UserTokenType;
import com.bookWise.user.service.repository.UserAuthRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider implements TokenProvider {
    public static final String CLAIM_KEY_USER_ID = "uid";
    public static final String CLAIM_KEY_TYPE = "typ";
    public static final String CLAIM_KEY_ISSUED_AT = "iat";
    public static final String CLAIM_KEY_JTI = "jti";

    private final JwtProperties jwtProperties;
    private final UserAuthRepository userAuthRepository;

    private Key key;
    private JwtParser jwtParser;

    @PostConstruct
    protected void init() {
        if (!StringUtils.hasText(jwtProperties.getSecretKey())) {
            throw new IllegalStateException("A chave secreta JWT não pode estar vazia.");
        }

        if (jwtProperties.getSecretKey().length() < 64) {
            log.warn("A chave secreta JWT deve ter pelo menos 64 caracteres para segurança adequada. Tamanho atual: {} caracteres.",
                    jwtProperties.getSecretKey().length());
        }

        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));

        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(key)
                .build();

        log.info("JwtProvider inicializado com sucesso.");
    }

    @Override
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, user.getId().toString());

        return buildToken(claims, user, UserTokenType.ACCESS);
    }

    @Override
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USER_ID, user.getId().toString());
        claims.put(CLAIM_KEY_TYPE, UserTokenType.REFRESH.name().toLowerCase());

        return buildToken(claims, user, UserTokenType.REFRESH);
    }

    private String buildToken(Map<String, Object> claims, User user, UserTokenType tokenType) {
        final long now = System.currentTimeMillis();
        final long expirationMs = tokenType == UserTokenType.REFRESH ?
                jwtProperties.getRefreshTokenExpirationMs() :
                jwtProperties.getAccessTokenExpirationMs();

        claims.put(CLAIM_KEY_ISSUED_AT, new Date(now));

        String tokenId = UUID.randomUUID().toString();
        claims.put(CLAIM_KEY_JTI, tokenId);

        log.debug("Gerando token do tipo {} para o usuário: {}", tokenType, user.getEmail());

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setExpiration(new Date(now + expirationMs))
                .setId(tokenId)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        saveUserToken(user, token, tokenType, new Date(now + expirationMs));

        return token;
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (!StringUtils.hasText(token) || token.split("\\.").length != 3) {
                log.warn("Token JWT inválido: formato incorreto");
                return false;
            }

            jwtParser.parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Token JWT não suportado: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Token JWT inválido: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("Assinatura JWT inválida: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Token JWT está vazio ou contém um argumento inválido: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro ao validar o token JWT: {}", ex.getMessage(), ex);
        }

        return false;
    }

    @Override
    public String getUsernameFromJWT(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    @Override
    public String getUserIdFromJWT(String token) {
        return getClaimFromToken(token, claims -> claims.get(CLAIM_KEY_USER_ID, String.class));
    }

    @Override
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    @Override
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public Claims getAllClaimsFromToken(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado, mas ainda podemos recuperar os claims: {}", ex.getMessage());
            return ex.getClaims();
        }
    }

    private void saveUserToken(User user, String token, UserTokenType tokenType, Date expiryDate) {
        try {
            UserToken userToken = new UserToken();
            userToken.setUser(user);
            userToken.setToken(token);
            userToken.setType(tokenType);
            userToken.setExpiresAt(expiryDate.toInstant());

            userAuthRepository.save(userToken);
        } catch (Exception e) {
            log.error("Error saving user token: {}", e.getMessage());
            throw new RuntimeException("Failed to save user token", e);
        }
    }
}
