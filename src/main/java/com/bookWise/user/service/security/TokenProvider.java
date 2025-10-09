package com.bookWise.user.service.security;

import com.bookWise.user.service.model.entity.User;
import io.jsonwebtoken.Claims;

import java.util.Date;
import java.util.function.Function;

public interface TokenProvider {
    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    boolean validateToken(String token);

    String getUsernameFromJWT(String token);

    String getUserIdFromJWT(String token);

    Date getExpirationDateFromToken(String token);

    <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver);

    Claims getAllClaimsFromToken(String token);
}
