package br.com.naheroback.modules.auth.services;

import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    @Value("${auth.jwt.token.secret}")
    private String secretKey;

    public String generateToken(Integer userId, List<String> roles, Integer expiration, String tokenType) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);

            return JWT.create()
                    .withIssuer("nahero-api")
                    .withClaim("roles", roles)
                    .withClaim("token_type", tokenType)
                    .withSubject(userId.toString())
                    .withExpiresAt(generateExpiration(expiration))
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            throw new UnauthorizedException();
        }
    }

    public DecodedJWT decodeAndValidateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secretKey);

            return JWT.require(algorithm)
                    .withIssuer("nahero-api")
                    .build()
                    .verify(token);

        } catch (JWTVerificationException exception) {
            throw new UnauthorizedException();
        }
    }

    public boolean isTokenExpired(DecodedJWT decodedJWT) {
        return decodedJWT.getExpiresAt().before(Date.from(Instant.now()));
    }

    public boolean isRefreshToken(DecodedJWT decodedJWT) {
        return "refresh".equals(decodedJWT.getClaim("token_type").asString());
    }

    private Instant generateExpiration(Integer expiration) {
        return LocalDateTime.now()
                .plusHours(expiration)
                .toInstant(ZoneOffset.of("-03:00"));
    }
}
