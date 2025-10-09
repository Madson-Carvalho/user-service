package com.bookWise.user.service.security;

import com.bookWise.user.service.exception.TokenValidationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/v1/v3/api-docs") ||
               requestURI.startsWith("/api/v1/swagger-ui") ||
               "/api/v1/swagger-ui.html".equals(requestURI) ||
               "/actuator/health".equals(requestURI) ||
               "/users/register".equals(requestURI) ||
               "/auth/login".equals(requestURI) ||
               "/auth/refresh".equals(requestURI);
    }

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, 
                                 CustomUserDetailsService userDetailsService, 
                                 @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.resolver = resolver;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("Processando requisição para: {}", requestURI);

        if (isPublicEndpoint(requestURI)) {
            log.debug("Rota pública acessada: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);
            if (jwt == null) {
                throw new TokenValidationException("Token JWT não fornecido");
            }
            
            if (jwtProvider.validateToken(jwt)) {
                String username = jwtProvider.getUsernameFromJWT(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Usuário autenticado: {}", username);
                
                filterChain.doFilter(request, response);
            } else {
                throw new TokenValidationException("Token JWT inválido");
            }
            
        } catch (ExpiredJwtException ex) {
            log.error("Token JWT expirado: {}", ex.getMessage());
            resolver.resolveException(request, response, null, 
                new TokenValidationException("Sessão expirada. Faça login novamente."));
                
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException ex) {
            log.error("Token JWT inválido: {}", ex.getMessage());
            resolver.resolveException(request, response, null, 
                new TokenValidationException("Token inválido"));
                
        } catch (TokenValidationException ex) {
            log.error("Erro de validação do token: {}", ex.getMessage());
            resolver.resolveException(request, response, null, ex);
            
        } catch (Exception ex) {
            log.error("Erro ao processar autenticação: {}", ex.getMessage(), ex);
            resolver.resolveException(request, response, null,
                new TokenValidationException("Erro interno na autenticação"));
        }
    }
}
