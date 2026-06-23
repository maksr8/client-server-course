package org.example.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtService {
    private static final String SECRET_KEY = "MySuperSecretJwtKey";
    private static final String ISSUER = "Store";
    private static final long EXPIRATION_TIME_MS = 3_600_000;

    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
    private final JWTVerifier verifier = JWT.require(algorithm).withIssuer(ISSUER).build();

    public String generateToken(String username) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                .sign(algorithm);
    }

    public String validateTokenAndGetUsername(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }
}